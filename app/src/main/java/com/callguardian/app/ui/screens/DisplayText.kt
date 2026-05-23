package com.callguardian.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.callguardian.app.R
import com.callguardian.app.core.model.AnonymousMode
import com.callguardian.app.core.model.CallAction
import com.callguardian.app.core.model.CountryStatus
import com.callguardian.app.core.model.ForeignCallMode
import com.callguardian.app.core.model.ProtectionLevel
import com.callguardian.app.core.model.RiskLevel
import com.callguardian.app.core.model.RuleAction
import com.callguardian.app.core.model.RuleType
import com.callguardian.app.core.model.ThemeMode
import com.callguardian.app.core.model.ThemePalette

fun ProtectionLevel.displayName(): String = when (this) {
    ProtectionLevel.OFF -> "Disattivata"
    ProtectionLevel.LIGHT -> "Leggera"
    ProtectionLevel.BALANCED -> "Bilanciata"
    ProtectionLevel.AGGRESSIVE -> "Aggressiva"
    ProtectionLevel.CUSTOM -> "Personalizzata"
}

fun AnonymousMode.displayName(): String = when (this) {
    AnonymousMode.WARN -> "Avvisa"
    AnonymousMode.SILENCE -> "Silenzia"
    AnonymousMode.BLOCK -> "Blocca"
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> "Consenti dopo tentativi ripetuti"
}

fun ForeignCallMode.displayName(): String = when (this) {
    ForeignCallMode.WARN_ONLY -> "Solo avviso"
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> "Blocca esteri sconosciuti"
    ForeignCallMode.BLOCK_ALL_FOREIGN -> "Blocca tutti gli esteri"
    ForeignCallMode.BLOCK_BY_COUNTRY -> "Blocca per nazione"
    ForeignCallMode.SCHEDULED -> "Programmata"
}

fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Chiaro"
    ThemeMode.DARK -> "Scuro"
}

fun ThemePalette.displayName(): String = when (this) {
    ThemePalette.SECURITY_BLUE -> "Blu sicurezza"
    ThemePalette.PROTECTION_GREEN -> "Verde protezione"
    ThemePalette.PROFESSIONAL_GRAY -> "Grigio professionale"
    ThemePalette.TECH_PURPLE -> "Viola tecnico"
}

fun CountryStatus.displayName(): String = when (this) {
    CountryStatus.ALLOWED -> "Consentita"
    CountryStatus.MONITORED -> "Monitorata"
    CountryStatus.BLOCKED -> "Bloccata"
}

fun CallAction.displayName(): String = when (this) {
    CallAction.ALLOWED -> "Consentita"
    CallAction.WARNED -> "Avviso"
    CallAction.SILENCED -> "Silenziata"
    CallAction.BLOCKED -> "Bloccata"
}

fun RiskLevel.displayName(): String = when (this) {
    RiskLevel.NORMAL -> "Normale"
    RiskLevel.SUSPICIOUS -> "Sospetta"
    RiskLevel.LIKELY_SPAM -> "Probabile spam"
}

fun RuleType.displayName(): String = when (this) {
    RuleType.WHITELIST -> "Numero consentito"
    RuleType.BLACKLIST_NUMBER -> "Numero bloccato"
    RuleType.PREFIX -> "Pattern iniziale"
    RuleType.RANGE -> "Intervallo"
    RuleType.COUNTRY -> "Nazione"
    RuleType.ANONYMOUS -> "Anonimo"
    RuleType.FOREIGN_UNKNOWN -> "Estero sconosciuto"
    RuleType.SCHEDULE -> "Orario"
}

fun RuleAction.displayName(): String = when (this) {
    RuleAction.ALLOW -> "Consenti"
    RuleAction.WARN -> "Avvisa"
    RuleAction.SILENCE -> "Silenzia"
    RuleAction.BLOCK -> "Blocca"
}

@StringRes
fun ProtectionLevel.stringRes(): Int = when (this) {
    ProtectionLevel.OFF -> R.string.protection_level_off
    ProtectionLevel.LIGHT -> R.string.protection_level_light
    ProtectionLevel.BALANCED -> R.string.protection_level_balanced
    ProtectionLevel.AGGRESSIVE -> R.string.protection_level_aggressive
    ProtectionLevel.CUSTOM -> R.string.protection_level_custom
}

@StringRes
fun AnonymousMode.stringRes(): Int = when (this) {
    AnonymousMode.WARN -> R.string.anonymous_mode_warn
    AnonymousMode.SILENCE -> R.string.anonymous_mode_silence
    AnonymousMode.BLOCK -> R.string.anonymous_mode_block
    AnonymousMode.ALLOW_AFTER_REPEATED_ATTEMPTS -> R.string.anonymous_mode_allow_repeated
}

@StringRes
fun ForeignCallMode.stringRes(): Int = when (this) {
    ForeignCallMode.WARN_ONLY -> R.string.foreign_mode_warn_only
    ForeignCallMode.BLOCK_UNKNOWN_FOREIGN -> R.string.foreign_mode_block_unknown
    ForeignCallMode.BLOCK_ALL_FOREIGN -> R.string.foreign_mode_block_all
    ForeignCallMode.BLOCK_BY_COUNTRY -> R.string.foreign_mode_block_country
    ForeignCallMode.SCHEDULED -> R.string.foreign_mode_scheduled
}

@StringRes
fun ThemeMode.stringRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

@StringRes
fun ThemePalette.stringRes(): Int = when (this) {
    ThemePalette.SECURITY_BLUE -> R.string.theme_palette_security_blue
    ThemePalette.PROTECTION_GREEN -> R.string.theme_palette_protection_green
    ThemePalette.PROFESSIONAL_GRAY -> R.string.theme_palette_professional_gray
    ThemePalette.TECH_PURPLE -> R.string.theme_palette_tech_purple
}

@StringRes
fun CountryStatus.stringRes(): Int = when (this) {
    CountryStatus.ALLOWED -> R.string.country_status_allowed
    CountryStatus.MONITORED -> R.string.country_status_monitored
    CountryStatus.BLOCKED -> R.string.country_status_blocked
}

@StringRes
fun CallAction.stringRes(): Int = when (this) {
    CallAction.ALLOWED -> R.string.call_action_allowed
    CallAction.WARNED -> R.string.call_action_warned
    CallAction.SILENCED -> R.string.call_action_silenced
    CallAction.BLOCKED -> R.string.call_action_blocked
}

@StringRes
fun RiskLevel.stringRes(): Int = when (this) {
    RiskLevel.NORMAL -> R.string.risk_level_normal
    RiskLevel.SUSPICIOUS -> R.string.risk_level_suspicious
    RiskLevel.LIKELY_SPAM -> R.string.risk_level_likely_spam
}

@StringRes
fun RuleType.stringRes(): Int = when (this) {
    RuleType.WHITELIST -> R.string.rule_type_whitelist
    RuleType.BLACKLIST_NUMBER -> R.string.rule_type_blacklist_number
    RuleType.PREFIX -> R.string.rule_type_prefix
    RuleType.RANGE -> R.string.rule_type_range
    RuleType.COUNTRY -> R.string.rule_type_country
    RuleType.ANONYMOUS -> R.string.rule_type_anonymous
    RuleType.FOREIGN_UNKNOWN -> R.string.rule_type_foreign_unknown
    RuleType.SCHEDULE -> R.string.rule_type_schedule
}

@StringRes
fun RuleAction.stringRes(): Int = when (this) {
    RuleAction.ALLOW -> R.string.rule_action_allow
    RuleAction.WARN -> R.string.rule_action_warn
    RuleAction.SILENCE -> R.string.rule_action_silence
    RuleAction.BLOCK -> R.string.rule_action_block
}

@Composable
fun ProtectionLevel.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun AnonymousMode.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun ForeignCallMode.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun ThemeMode.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun ThemePalette.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun CountryStatus.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun CallAction.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun RiskLevel.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun RuleType.localizedDisplayName(): String = stringResource(stringRes())

@Composable
fun RuleAction.localizedDisplayName(): String = stringResource(stringRes())
