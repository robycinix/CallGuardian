package com.callguardian.app.core.model

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

enum class RuleType { WHITELIST, BLACKLIST_NUMBER, PREFIX, RANGE, COUNTRY, ANONYMOUS, FOREIGN_UNKNOWN, SCHEDULE }
enum class RuleAction { ALLOW, WARN, SILENCE, BLOCK }
enum class ProtectionLevel { OFF, LIGHT, BALANCED, AGGRESSIVE, CUSTOM }
enum class AnonymousMode { WARN, SILENCE, BLOCK, ALLOW_AFTER_REPEATED_ATTEMPTS }
enum class CountryStatus { ALLOWED, MONITORED, BLOCKED }
enum class ForeignCallMode { WARN_ONLY, BLOCK_UNKNOWN_FOREIGN, BLOCK_ALL_FOREIGN, BLOCK_BY_COUNTRY, SCHEDULED }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class ThemePalette { SECURITY_BLUE, PROTECTION_GREEN, PROFESSIONAL_GRAY, TECH_PURPLE }
enum class CallAction { ALLOWED, WARNED, SILENCED, BLOCKED }
enum class RiskLevel { NORMAL, SUSPICIOUS, LIKELY_SPAM }

data class CallDecision(
    val action: CallAction,
    val score: Int,
    val riskLevel: RiskLevel,
    val reason: String,
    val matchedRuleId: Long? = null,
    val countryIso: String? = null,
    val blockGroupId: Long? = null,
    val blockGroupName: String? = null,
    val blockGroupContactName: String? = null,
)

data class ContactPhoneSelection(
    val displayName: String,
    val phoneNumber: String,
    val contactId: Long? = null,
    val contactLookupKey: String? = null,
)

data class PermissionSummary(
    val runtimePermissionsGranted: Boolean,
    val callScreeningRoleHeld: Boolean,
    val overlayAllowed: Boolean,
    val notificationPermissionGranted: Boolean,
)

data class CountryDialingInfo(
    val iso: String,
    val name: String,
    val dialCode: String,
    val flag: String,
)

val SupportedCountries: List<CountryDialingInfo> = buildSupportedCountries()

private fun buildSupportedCountries(): List<CountryDialingInfo> {
    val phoneNumberUtil = PhoneNumberUtil.getInstance()
    val italianLocale = Locale.ITALIAN

    return phoneNumberUtil.supportedRegions
        .map { iso ->
            CountryDialingInfo(
                iso = iso,
                name = Locale("", iso).getDisplayCountry(italianLocale).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(italianLocale) else char.toString()
                },
                dialCode = "+${phoneNumberUtil.getCountryCodeForRegion(iso)}",
                flag = iso.toFlagEmoji(),
            )
        }
        .sortedWith(compareBy<CountryDialingInfo> { it.name }.thenBy { it.dialCode })
}

private fun String.toFlagEmoji(): String {
    if (length != 2) return ""
    return uppercase(Locale.US)
        .map { char -> Character.toChars(REGIONAL_INDICATOR_BASE + (char.code - 'A'.code)).concatToString() }
        .joinToString(separator = "")
}

private const val REGIONAL_INDICATOR_BASE = 0x1F1E6
