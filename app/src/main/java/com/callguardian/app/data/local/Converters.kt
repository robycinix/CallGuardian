package com.callguardian.app.data.local

import androidx.room.TypeConverter
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

class Converters {
    @TypeConverter fun toRuleType(value: String): RuleType = enumValueOf(value)
    @TypeConverter fun fromRuleType(value: RuleType): String = value.name
    @TypeConverter fun toRuleAction(value: String): RuleAction = enumValueOf(value)
    @TypeConverter fun fromRuleAction(value: RuleAction): String = value.name
    @TypeConverter fun toCallAction(value: String): CallAction = enumValueOf(value)
    @TypeConverter fun fromCallAction(value: CallAction): String = value.name
    @TypeConverter fun toRiskLevel(value: String): RiskLevel = enumValueOf(value)
    @TypeConverter fun fromRiskLevel(value: RiskLevel): String = value.name
    @TypeConverter fun toCountryStatus(value: String): CountryStatus = enumValueOf(value)
    @TypeConverter fun fromCountryStatus(value: CountryStatus): String = value.name
    @TypeConverter fun toProtectionLevel(value: String): ProtectionLevel = enumValueOf(value)
    @TypeConverter fun fromProtectionLevel(value: ProtectionLevel): String = value.name
    @TypeConverter fun toAnonymousMode(value: String): AnonymousMode = enumValueOf(value)
    @TypeConverter fun fromAnonymousMode(value: AnonymousMode): String = value.name
    @TypeConverter fun toForeignCallMode(value: String): ForeignCallMode = enumValueOf(value)
    @TypeConverter fun fromForeignCallMode(value: ForeignCallMode): String = value.name
    @TypeConverter fun toThemeMode(value: String): ThemeMode = enumValueOf(value)
    @TypeConverter fun fromThemeMode(value: ThemeMode): String = value.name
    @TypeConverter fun toThemePalette(value: String): ThemePalette = enumValueOf(value)
    @TypeConverter fun fromThemePalette(value: ThemePalette): String = value.name
}
