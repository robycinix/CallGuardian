package com.callguardian.app.data.local

import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.core.model.SupportedCountries
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val dao: CallGuardianDao,
) {
    suspend fun seedIfNeeded() {
        if (dao.settings() == null) {
            dao.upsertSettings(AppSettingsEntity())
        }
        val existingCountryIsos = dao.countryRuleIsos().toSet()
        val missingCountries = SupportedCountries
            .filterNot { it.iso in existingCountryIsos }
            .map { country ->
                    CountryRuleEntity(
                        iso = country.iso,
                        name = country.name,
                        dialCode = country.dialCode,
                        flag = country.flag,
                        status = if (country.iso == "IT") CountryStatus.ALLOWED else CountryStatus.MONITORED,
                    )
            }
        if (missingCountries.isNotEmpty()) {
            dao.upsertCountryRules(missingCountries)
        }
        if (dao.allRules().isEmpty()) {
            dao.upsertRule(
                RuleEntity(
                    label = "Avvisa numeri anonimi",
                    value = "anonymous",
                    type = RuleType.ANONYMOUS,
                    action = RuleAction.WARN,
                )
            )
        }
    }
}
