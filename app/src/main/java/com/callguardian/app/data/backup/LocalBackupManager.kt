package com.callguardian.app.data.backup

import com.callguardian.app.data.local.AppSettingsEntity
import com.callguardian.app.data.local.CallGuardianDao
import com.callguardian.app.data.local.CountryRuleEntity
import com.callguardian.app.data.local.EventLogEntity
import com.callguardian.app.data.local.RuleEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    private val dao: CallGuardianDao,
    private val gson: Gson,
) {
    suspend fun exportJson(): String = gson.toJson(
        BackupPayload(
            version = 1,
            settings = dao.settings() ?: AppSettingsEntity(),
            rules = dao.allRules(),
            events = dao.recentEvents(500),
            countries = dao.allCountryRules(),
        )
    )

    suspend fun importJson(json: String) {
        val payload = gson.fromJson(json, BackupPayload::class.java)
            ?: error("Backup non valido")
        require(payload.version == 1) { "Versione backup non supportata: ${payload.version}" }
        dao.restoreBackup(
            settings = payload.settings.copy(id = 1),
            rules = payload.rules,
            events = payload.events,
            countries = payload.countries,
        )
    }

    data class BackupPayload(
        @SerializedName("version") val version: Int,
        @SerializedName("settings") val settings: AppSettingsEntity,
        @SerializedName("rules") val rules: List<RuleEntity>,
        @SerializedName("events") val events: List<EventLogEntity>,
        @SerializedName("countries") val countries: List<CountryRuleEntity>,
    )
}
