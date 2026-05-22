package com.callguardian.app.ui.screens

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
