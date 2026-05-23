package com.callguardian.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val value: String,
    val type: RuleType,
    val action: RuleAction,
    val enabled: Boolean = true,
    val priority: Int = defaultPriority(type),
    val startsAtMinute: Int? = null,
    val endsAtMinute: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    companion object {
        fun defaultPriority(type: RuleType): Int = when (type) {
            RuleType.WHITELIST -> 10
            RuleType.BLACKLIST_NUMBER -> 40
            RuleType.PREFIX, RuleType.RANGE, RuleType.COUNTRY -> 50
            RuleType.ANONYMOUS, RuleType.FOREIGN_UNKNOWN, RuleType.SCHEDULE -> 60
        }
    }
}

@Entity(tableName = "block_groups")
data class BlockGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "block_group_members",
    foreignKeys = [
        ForeignKey(
            entity = BlockGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("groupId"),
        Index(value = ["groupId", "normalizedNumber"], unique = true),
        Index("normalizedNumber"),
    ],
)
data class BlockGroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val contactId: Long? = null,
    val contactLookupKey: String? = null,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactName: String? = null,
    val action: CallAction,
    val riskLevel: RiskLevel,
    val score: Int,
    val reason: String,
    val matchedRuleId: Long? = null,
    val countryIso: String? = null,
)

@Entity(tableName = "stats_events")
data class StatsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val phoneNumber: String,
    val normalizedNumber: String,
    val action: CallAction,
    val riskLevel: RiskLevel,
    val score: Int,
    val reason: String,
    val matchedRuleId: Long? = null,
    val countryIso: String? = null,
)

@Entity(tableName = "country_rules")
data class CountryRuleEntity(
    @PrimaryKey val iso: String,
    val name: String,
    val dialCode: String,
    val flag: String,
    val status: CountryStatus = CountryStatus.MONITORED,
)

@Entity(tableName = "settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val protectionLevel: ProtectionLevel = ProtectionLevel.BALANCED,
    val anonymousMode: AnonymousMode = AnonymousMode.WARN,
    val foreignCallMode: ForeignCallMode = ForeignCallMode.BLOCK_UNKNOWN_FOREIGN,
    val blockForeignUnknownNotInContacts: Boolean = true,
    val allowRepeatedAnonymousAfterAttempts: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.SECURITY_BLUE,
    val highContrast: Boolean = false,
    val contextualHelpEnabled: Boolean = true,
    val languageCode: String = "system",
)
