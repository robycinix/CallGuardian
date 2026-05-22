package com.callguardian.app.data.repository

import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CallDecision
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.RiskLevel
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.RuleEntity
import com.callguardian.app.telephony.PhoneNumberNormalizer
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallClassifier @Inject constructor(
    private val normalizer: PhoneNumberNormalizer,
) {
    fun classify(
        rawNumber: String?,
        settings: AppSettingsEntity,
        rules: List<RuleEntity>,
        countryRule: CountryRuleEntity?,
        existsInContacts: Boolean,
        recentSimilarCalls: Int,
    ): CallDecision {
        val normalized = normalizer.normalize(rawNumber)
        val country = normalizer.countryFor(normalized)
        val isAnonymous = normalized == PhoneNumberNormalizer.ANONYMOUS
        val isForeign = normalizer.isForeign(normalized)

        if (settings.protectionLevel == ProtectionLevel.OFF) {
            return CallDecision(CallAction.ALLOWED, 0, RiskLevel.NORMAL, "Protezione disattivata", countryIso = country?.iso)
        }

        rules.firstOrNull { it.enabled && it.type == RuleType.WHITELIST && matchesRule(it, normalized) }?.let {
            return CallDecision(CallAction.ALLOWED, 0, RiskLevel.NORMAL, "Numero nella lista consentiti", it.id, country?.iso)
        }
        if (existsInContacts) {
            return CallDecision(CallAction.ALLOWED, 0, RiskLevel.NORMAL, "Numero presente in rubrica", countryIso = country?.iso)
        }

        var score = baseScore(settings.protectionLevel)
        var reason = "Nessuna regola critica"
        var matchedRuleId: Long? = null
        var explicitRuleAction: RuleAction? = null

        val directRule = rules.firstOrNull {
            it.enabled && it.type == RuleType.BLACKLIST_NUMBER && matchesRule(it, normalized)
        }
        if (directRule != null) {
            score += 100
            reason = "Numero nella lista bloccati"
            matchedRuleId = directRule.id
            explicitRuleAction = directRule.action
        }

        val rangeRule = rules.firstOrNull {
            it.enabled && (it.type == RuleType.PREFIX || it.type == RuleType.RANGE || it.type == RuleType.COUNTRY) && matchesRule(it, normalized)
        }
        if (rangeRule != null) {
            score += when (rangeRule.type) {
                RuleType.PREFIX, RuleType.RANGE -> 40
                RuleType.COUNTRY -> 45
                else -> 0
            }
            reason = "Regola ${rangeRule.label}"
            matchedRuleId = rangeRule.id
            explicitRuleAction = rangeRule.action
        }

        if (isAnonymous) {
            score += 50
            reason = "Numero anonimo"
        }
        if (isForeign && !existsInContacts) {
            score += 25
            reason = "Numero estero sconosciuto"
        }
        if (recentSimilarCalls >= 2) {
            score += 30
            reason = "Chiamate ravvicinate dallo stesso numero"
        }
        val scheduleRule = if (settings.foreignCallMode == ForeignCallMode.SCHEDULED && isForeign) {
            activeScheduleRule(rules)
        } else {
            null
        }
        val scheduledForeignRulesActive = scheduleRule != null
        if (scheduledForeignRulesActive) {
            score += 45
            reason = "Programmazione esteri attiva"
            matchedRuleId = scheduleRule?.id
        }

        countryRule?.let {
            when (countryRuleMeaning(settings.foreignCallMode, it.status, scheduledForeignRulesActive)) {
                CountryRuleMeaning.ALLOW_EXCEPTION -> {
                    score -= 45
                    reason = "Nazione esclusa dal blocco esteri: ${it.name}"
                }
                CountryRuleMeaning.BLOCK_COUNTRY -> {
                    score += 60
                    reason = "Nazione bloccata: ${it.name}"
                }
                CountryRuleMeaning.MONITOR -> Unit
            }
        }

        if (!existsInContacts && !isAnonymous && !isForeign && matchedRuleId == null && score < 31) {
            score = 35
            reason = "Numero non presente in rubrica"
        }

        val action = decideAction(
            score = score,
            settings = settings,
            isAnonymous = isAnonymous,
            isForeign = isForeign,
            countryRule = countryRule,
            recentSimilarCalls = recentSimilarCalls,
            explicitRuleAction = explicitRuleAction,
            scheduledForeignRulesActive = scheduledForeignRulesActive,
        )
        val riskLevel = when {
            score <= 30 -> RiskLevel.NORMAL
            score <= 70 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.LIKELY_SPAM
        }
        return CallDecision(action, score.coerceAtLeast(0), riskLevel, reason, matchedRuleId, country?.iso)
    }

    private fun baseScore(level: ProtectionLevel): Int = when (level) {
        ProtectionLevel.OFF -> 0
        ProtectionLevel.LIGHT -> 0
        ProtectionLevel.BALANCED -> 10
        ProtectionLevel.AGGRESSIVE -> 25
        ProtectionLevel.CUSTOM -> 10
    }

    private fun decideAction(
        score: Int,
        settings: AppSettingsEntity,
        isAnonymous: Boolean,
        isForeign: Boolean,
        countryRule: CountryRuleEntity?,
        recentSimilarCalls: Int,
        explicitRuleAction: RuleAction?,
        scheduledForeignRulesActive: Boolean,
    ): CallAction {
        explicitRuleAction?.let {
            return when (it) {
                RuleAction.ALLOW -> CallAction.ALLOWED
                RuleAction.WARN -> CallAction.WARNED
                RuleAction.SILENCE -> CallAction.SILENCED
                RuleAction.BLOCK -> CallAction.BLOCKED
            }
        }
        if (isAnonymous) {
            return when (settings.anonymousMode) {
                AnonymousMode.BLOCK -> CallAction.BLOCKED
                AnonymousMode.SILENCE -> CallAction.SILENCED
                AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> {
                    if (recentSimilarCalls >= settings.allowRepeatedAnonymousAfterAttempts) {
                        CallAction.ALLOWED
                    } else {
                        CallAction.WARNED
                    }
                }
                AnonymousMode.WARN -> CallAction.WARNED
            }
        }
        if (isForeign) {
            when (settings.foreignCallMode) {
                ForeignCallMode.WARN_ONLY -> return CallAction.WARNED
                ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> {
                    if (countryRule?.status == CountryStatus.ALLOWED) return CallAction.ALLOWED
                    if (!settings.blockForeignUnknownNotInContacts || !isForeign) return CallAction.WARNED
                    return CallAction.BLOCKED
                }
                ForeignCallMode.BLOCK_ALL_FOREIGN -> {
                    if (countryRule?.status == CountryStatus.ALLOWED) return CallAction.ALLOWED
                    return CallAction.BLOCKED
                }
                ForeignCallMode.BLOCK_BY_COUNTRY -> if (countryRule?.status == CountryStatus.BLOCKED) return CallAction.BLOCKED
                ForeignCallMode.SCHEDULED -> {
                    if (scheduledForeignRulesActive && countryRule?.status == CountryStatus.ALLOWED) return CallAction.ALLOWED
                    if (scheduledForeignRulesActive && countryRule?.status == CountryStatus.BLOCKED) return CallAction.BLOCKED
                    return CallAction.WARNED
                }
            }
        }
        return when {
            score >= 71 -> CallAction.BLOCKED
            score >= 31 -> CallAction.WARNED
            else -> CallAction.ALLOWED
        }
    }

    private fun matchesRule(rule: RuleEntity, normalized: String): Boolean = when (rule.type) {
        RuleType.WHITELIST, RuleType.BLACKLIST_NUMBER -> normalized == rule.value
        RuleType.PREFIX -> normalized.startsWith(rule.value)
        RuleType.RANGE -> matchesRange(rule.value, normalized)
        RuleType.COUNTRY -> normalized.startsWith(rule.value)
        RuleType.ANONYMOUS -> normalized == PhoneNumberNormalizer.ANONYMOUS
        RuleType.FOREIGN_UNKNOWN -> normalizer.isForeign(normalized)
        RuleType.SCHEDULE -> false
    }

    private fun matchesRange(ruleValue: String, normalized: String): Boolean {
        val bounds = ruleValue.split("-", limit = 2)
        if (bounds.size != 2) return normalized.startsWith(ruleValue)

        val start = bounds[0].digitsOnly()
        val end = bounds[1].digitsOnly()
        val number = normalized.digitsOnly()
        if (start.isEmpty() || end.isEmpty() || number.isEmpty()) return false
        if (number.length != start.length || number.length != end.length) return false

        val startNumber = start.toBigIntegerOrNull() ?: return false
        val endNumber = end.toBigIntegerOrNull() ?: return false
        val candidate = number.toBigIntegerOrNull() ?: return false
        val lower = startNumber.min(endNumber)
        val upper = startNumber.max(endNumber)
        return candidate in lower..upper
    }

    private fun String.digitsOnly(): String = filter(Char::isDigit)

    private fun activeScheduleRule(rules: List<RuleEntity>): RuleEntity? {
        val minuteOfDay = LocalTime.now().hour * 60 + LocalTime.now().minute
        return rules.firstOrNull { rule ->
            rule.enabled &&
                rule.type == RuleType.SCHEDULE &&
                rule.startsAtMinute != null &&
                rule.endsAtMinute != null &&
                if (rule.startsAtMinute <= rule.endsAtMinute) {
                    minuteOfDay in rule.startsAtMinute..rule.endsAtMinute
                } else {
                    minuteOfDay >= rule.startsAtMinute || minuteOfDay <= rule.endsAtMinute
                }
        }
    }

    private fun countryRuleMeaning(
        mode: ForeignCallMode,
        status: CountryStatus,
        scheduledForeignRulesActive: Boolean,
    ): CountryRuleMeaning = when (mode) {
        ForeignCallMode.BLOCK_UNKNOWN_FOREIGN, ForeignCallMode.BLOCK_ALL_FOREIGN ->
            if (status == CountryStatus.ALLOWED) CountryRuleMeaning.ALLOW_EXCEPTION else CountryRuleMeaning.MONITOR
        ForeignCallMode.BLOCK_BY_COUNTRY ->
            if (status == CountryStatus.BLOCKED) CountryRuleMeaning.BLOCK_COUNTRY else CountryRuleMeaning.MONITOR
        ForeignCallMode.WARN_ONLY -> when (status) {
            CountryStatus.ALLOWED -> CountryRuleMeaning.ALLOW_EXCEPTION
            CountryStatus.BLOCKED -> CountryRuleMeaning.BLOCK_COUNTRY
            CountryStatus.MONITORED -> CountryRuleMeaning.MONITOR
        }
        ForeignCallMode.SCHEDULED ->
            if (!scheduledForeignRulesActive) {
                CountryRuleMeaning.MONITOR
            } else {
                when (status) {
                    CountryStatus.ALLOWED -> CountryRuleMeaning.ALLOW_EXCEPTION
                    CountryStatus.BLOCKED -> CountryRuleMeaning.BLOCK_COUNTRY
                    CountryStatus.MONITORED -> CountryRuleMeaning.MONITOR
                }
            }
    }

    private enum class CountryRuleMeaning {
        ALLOW_EXCEPTION,
        BLOCK_COUNTRY,
        MONITOR,
    }
}
