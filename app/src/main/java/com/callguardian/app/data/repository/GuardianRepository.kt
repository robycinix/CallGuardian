package com.callguardian.app.data.repository

import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.local.CallGuardianDao
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.DatabaseSeeder
import com.callguardian.app.data.local.EventLogEntity
import com.callguardian.app.data.local.StatsEventEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.telephony.ContactLookup
import com.callguardian.app.telephony.PhoneNumberNormalizer
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class GuardianRepository @Inject constructor(
    private val dao: CallGuardianDao,
    private val seeder: DatabaseSeeder,
    private val normalizer: PhoneNumberNormalizer,
    private val contactLookup: ContactLookup,
    private val classifier: CallClassifier,
) {
    val rules: Flow<List<RuleEntity>> = dao.observeRules()
    val events: Flow<List<EventLogEntity>> = dao.observeRecentEvents(MAX_EVENT_LOGS)
    val statsEvents: Flow<List<StatsEventEntity>> = dao.observeStatsEvents()
    val countryRules: Flow<List<CountryRuleEntity>> = dao.observeCountryRules()
    val settings: Flow<AppSettingsEntity> = dao.observeSettings().map { it ?: AppSettingsEntity() }
    val blockedToday: Flow<Int> = dao.observeStatsActionCountSince(CallAction.BLOCKED, todayStartMillis())
    val topBlockedCountries = dao.observeTopStatsCountries(CallAction.BLOCKED, 8)
    private val initializationMutex = Mutex()
    @Volatile private var initialized = false
    @Volatile private var cachedSettings: AppSettingsEntity? = null

    suspend fun initialize() {
        if (initialized) {
            if (cachedSettings == null) {
                refreshSettingsSnapshot()
            }
            return
        }
        initializationMutex.withLock {
            if (!initialized) {
                seeder.seedIfNeeded()
                refreshSettingsSnapshot()
                initialized = true
            }
        }
    }

    fun initialSettings(): AppSettingsEntity = cachedSettings ?: AppSettingsEntity()

    suspend fun refreshSettingsSnapshot(): AppSettingsEntity {
        val settings = dao.settings() ?: AppSettingsEntity()
        cachedSettings = settings
        return settings
    }

    suspend fun evaluateIncomingCall(rawNumber: String?): CallDecision {
        val normalized = normalizer.normalize(rawNumber)
        val country = normalizer.countryFor(normalized)
        val settings = dao.settings() ?: AppSettingsEntity()
        val rules = dao.enabledRules()
        val recentSimilar = dao.recentEvents(20).count { it.normalizedNumber == normalized }
        val contact = contactLookup.lookup(normalized)
        val decision = classifier.classify(
            rawNumber = rawNumber,
            settings = settings,
            rules = rules,
            countryRule = country?.let { dao.countryRule(it.iso) },
            existsInContacts = contact != null,
            recentSimilarCalls = recentSimilar,
        )
        dao.insertEventWithStats(
            EventLogEntity(
                phoneNumber = rawNumber.orEmpty().ifBlank { "Anonimo" },
                normalizedNumber = normalized,
                contactName = contact?.displayName,
                action = decision.action,
                riskLevel = decision.riskLevel,
                score = decision.score,
                reason = decision.reason,
                matchedRuleId = decision.matchedRuleId,
                countryIso = decision.countryIso,
            )
        )
        return decision
    }

    suspend fun previewCallDecision(rawNumber: String?): CallDecision {
        val normalized = normalizer.normalize(rawNumber)
        val country = normalizer.countryFor(normalized)
        val settings = dao.settings() ?: AppSettingsEntity()
        val rules = dao.enabledRules()
        val recentSimilar = dao.recentEvents(20).count { it.normalizedNumber == normalized }
        val contact = contactLookup.lookup(normalized)
        return classifier.classify(
            rawNumber = rawNumber,
            settings = settings,
            rules = rules,
            countryRule = country?.let { dao.countryRule(it.iso) },
            existsInContacts = contact != null,
            recentSimilarCalls = recentSimilar,
        )
    }

    suspend fun addNumberRule(label: String, number: String, action: RuleAction) {
        val normalized = normalizer.normalize(number)
        val type = if (action == RuleAction.ALLOW) RuleType.WHITELIST else RuleType.BLACKLIST_NUMBER
        dao.upsertRule(RuleEntity(label = label, value = normalized, type = type, action = action))
    }

    suspend fun addPrefixRule(label: String, prefix: String, action: RuleAction = RuleAction.BLOCK) {
        dao.upsertRule(RuleEntity(label = label, value = normalizer.normalize(prefix), type = RuleType.PREFIX, action = action))
    }

    suspend fun addScheduleRule(label: String, startsAtMinute: Int, endsAtMinute: Int, action: RuleAction = RuleAction.BLOCK) {
        dao.upsertRule(
            RuleEntity(
                label = label,
                value = "$startsAtMinute-$endsAtMinute",
                type = RuleType.SCHEDULE,
                action = action,
                startsAtMinute = startsAtMinute,
                endsAtMinute = endsAtMinute,
            )
        )
    }

    suspend fun addCountryRule(rule: CountryRuleEntity) {
        dao.upsertCountryRule(rule)
    }

    suspend fun updateCountryStatus(rule: CountryRuleEntity, status: CountryStatus) {
        dao.upsertCountryRule(rule.copy(status = status))
    }

    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        dao.setRuleEnabled(ruleId, enabled)
    }

    suspend fun deleteRule(rule: RuleEntity) {
        dao.deleteRule(rule)
    }

    suspend fun deleteRules(ruleIds: Collection<Long>) {
        if (ruleIds.isNotEmpty()) {
            dao.deleteRulesByIds(ruleIds.toList())
        }
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.upsertSettings(settings)
        cachedSettings = settings
    }

    suspend fun applyRecommendedSetup() {
        val current = dao.settings() ?: AppSettingsEntity()
        dao.upsertSettings(
            current.copy(
                protectionLevel = ProtectionLevel.BALANCED,
                anonymousMode = AnonymousMode.WARN,
                foreignCallMode = ForeignCallMode.BLOCK_UNKNOWN_FOREIGN,
                blockForeignUnknownNotInContacts = true,
                allowRepeatedAnonymousAfterAttempts = 3,
            )
        )
    }

    suspend fun deleteEvent(event: EventLogEntity) {
        dao.deleteEventById(event.id)
    }

    suspend fun deleteEvents(eventIds: Collection<Long>) {
        if (eventIds.isNotEmpty()) {
            dao.deleteEventsByIds(eventIds.toList())
        }
    }

    suspend fun deleteAllEvents() {
        dao.deleteAllEvents()
    }

    suspend fun resetStats() {
        dao.deleteAllStatsEvents()
    }

    suspend fun cleanupEventLogs() {
        dao.pruneEvents(logRetentionThresholdMillis(), MAX_EVENT_LOGS)
    }

    suspend fun recentEventsSnapshot(): List<EventLogEntity> = dao.recentEvents(500)

    private fun todayStartMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun logRetentionThresholdMillis(): Long =
        System.currentTimeMillis() - LOG_RETENTION_DAYS * MILLIS_PER_DAY

    private companion object {
        const val LOG_RETENTION_DAYS = 180L
        const val MAX_EVENT_LOGS = 1_000
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
