package com.callguardian.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.callguardian.app.core.model.CallAction
import kotlinx.coroutines.flow.Flow

@Dao
interface CallGuardianDao {
    @Query("SELECT * FROM rules ORDER BY priority ASC, updatedAtMillis DESC")
    fun observeRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC, updatedAtMillis DESC")
    suspend fun enabledRules(): List<RuleEntity>

    @Query("SELECT * FROM rules ORDER BY priority ASC, updatedAtMillis DESC")
    suspend fun allRules(): List<RuleEntity>

    @Query("SELECT * FROM block_groups ORDER BY updatedAtMillis DESC, name COLLATE NOCASE ASC")
    fun observeBlockGroups(): Flow<List<BlockGroupEntity>>

    @Query("SELECT * FROM block_group_members ORDER BY displayName COLLATE NOCASE ASC, phoneNumber ASC")
    fun observeBlockGroupMembers(): Flow<List<BlockGroupMemberEntity>>

    @Query("SELECT * FROM block_groups ORDER BY updatedAtMillis DESC, name COLLATE NOCASE ASC")
    suspend fun allBlockGroups(): List<BlockGroupEntity>

    @Query("SELECT * FROM block_group_members ORDER BY displayName COLLATE NOCASE ASC, phoneNumber ASC")
    suspend fun allBlockGroupMembers(): List<BlockGroupMemberEntity>

    @Query("SELECT * FROM block_groups WHERE id = :groupId LIMIT 1")
    suspend fun blockGroup(groupId: Long): BlockGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockGroup(group: BlockGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockGroups(groups: List<BlockGroupEntity>)

    @Update
    suspend fun updateBlockGroup(group: BlockGroupEntity)

    @Delete
    suspend fun deleteBlockGroup(group: BlockGroupEntity)

    @Query("DELETE FROM block_groups WHERE id = :groupId")
    suspend fun deleteBlockGroupById(groupId: Long): Int

    @Query("DELETE FROM block_groups")
    suspend fun deleteAllBlockGroups(): Int

    @Query("UPDATE block_groups SET enabled = :enabled, updatedAtMillis = :updatedAt WHERE id = :groupId")
    suspend fun setBlockGroupEnabled(groupId: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockGroupMember(member: BlockGroupMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockGroupMembers(members: List<BlockGroupMemberEntity>)

    @Query("DELETE FROM block_group_members WHERE id = :memberId")
    suspend fun deleteBlockGroupMemberById(memberId: Long): Int

    @Query("DELETE FROM block_group_members WHERE id IN (:memberIds)")
    suspend fun deleteBlockGroupMembersByIds(memberIds: List<Long>): Int

    @Query("DELETE FROM block_group_members WHERE groupId = :groupId")
    suspend fun deleteBlockGroupMembers(groupId: Long): Int

    @Query("DELETE FROM block_group_members")
    suspend fun deleteAllBlockGroupMembers(): Int

    @Query(
        """
        SELECT
            g.id AS groupId,
            g.name AS groupName,
            m.displayName AS contactName,
            m.phoneNumber AS phoneNumber
        FROM block_group_members m
        INNER JOIN block_groups g ON g.id = m.groupId
        WHERE g.enabled = 1 AND m.normalizedNumber = :normalizedNumber
        ORDER BY g.updatedAtMillis DESC, g.id DESC
        LIMIT 1
        """
    )
    suspend fun enabledBlockGroupMatch(normalizedNumber: String): BlockGroupMatchRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: RuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<RuleEntity>)

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE value IN (:values)")
    suspend fun deleteRulesByValues(values: List<String>): Int

    @Query("DELETE FROM rules WHERE id IN (:ruleIds)")
    suspend fun deleteRulesByIds(ruleIds: List<Long>): Int

    @Query("DELETE FROM rules")
    suspend fun deleteAllRules(): Int

    @Query("UPDATE rules SET enabled = :enabled, updatedAtMillis = :updatedAt WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM event_logs ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 100): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_logs ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun recentEvents(limit: Int = 100): List<EventLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatsEvent(event: StatsEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatsEvents(events: List<StatsEventEntity>)

    @Transaction
    suspend fun insertEventWithStats(event: EventLogEntity) {
        insertEvent(event)
        insertStatsEvent(event.toStatsEvent())
    }

    @Query("DELETE FROM event_logs WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long): Int

    @Query("DELETE FROM event_logs WHERE id IN (:eventIds)")
    suspend fun deleteEventsByIds(eventIds: List<Long>): Int

    @Query("DELETE FROM event_logs")
    suspend fun deleteAllEvents(): Int

    @Query("SELECT COUNT(*) FROM event_logs WHERE action = :action AND timestampMillis >= :sinceMillis")
    fun observeActionCountSince(action: CallAction, sinceMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM stats_events WHERE action = :action AND timestampMillis >= :sinceMillis")
    fun observeStatsActionCountSince(action: CallAction, sinceMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM event_logs WHERE action = :action AND timestampMillis >= :sinceMillis")
    suspend fun actionCountSince(action: CallAction, sinceMillis: Long): Int

    @Query("DELETE FROM event_logs WHERE timestampMillis < :thresholdMillis")
    suspend fun deleteEventsOlderThan(thresholdMillis: Long): Int

    @Query("DELETE FROM event_logs WHERE id NOT IN (SELECT id FROM event_logs ORDER BY timestampMillis DESC LIMIT :maxEvents)")
    suspend fun trimEventsToMostRecent(maxEvents: Int): Int

    @Transaction
    suspend fun pruneEvents(thresholdMillis: Long, maxEvents: Int) {
        deleteEventsOlderThan(thresholdMillis)
        trimEventsToMostRecent(maxEvents)
    }

    @Query("SELECT countryIso, COUNT(*) as total FROM event_logs WHERE action = :action AND countryIso IS NOT NULL GROUP BY countryIso ORDER BY total DESC LIMIT :limit")
    fun observeTopCountries(action: CallAction, limit: Int = 5): Flow<List<CountryStatRow>>

    @Query("SELECT countryIso, COUNT(*) as total FROM stats_events WHERE action = :action AND countryIso IS NOT NULL GROUP BY countryIso ORDER BY total DESC LIMIT :limit")
    fun observeTopStatsCountries(action: CallAction, limit: Int = 5): Flow<List<CountryStatRow>>

    @Query("SELECT * FROM stats_events ORDER BY timestampMillis DESC")
    fun observeStatsEvents(): Flow<List<StatsEventEntity>>

    @Query("DELETE FROM stats_events")
    suspend fun deleteAllStatsEvents(): Int

    @Query("SELECT * FROM country_rules ORDER BY name ASC")
    fun observeCountryRules(): Flow<List<CountryRuleEntity>>

    @Query("SELECT * FROM country_rules ORDER BY name ASC")
    suspend fun allCountryRules(): List<CountryRuleEntity>

    @Query("SELECT iso FROM country_rules")
    suspend fun countryRuleIsos(): List<String>

    @Query("SELECT * FROM country_rules WHERE iso = :iso LIMIT 1")
    suspend fun countryRule(iso: String): CountryRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountryRule(rule: CountryRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCountryRules(rules: List<CountryRuleEntity>)

    @Query("DELETE FROM country_rules")
    suspend fun deleteAllCountryRules(): Int

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun settings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Transaction
    suspend fun restoreBackup(
        settings: AppSettingsEntity,
        rules: List<RuleEntity>,
        events: List<EventLogEntity>,
        countries: List<CountryRuleEntity>,
        blockGroups: List<BlockGroupEntity>,
        blockGroupMembers: List<BlockGroupMemberEntity>,
    ) {
        upsertSettings(settings)
        deleteAllBlockGroupMembers()
        deleteAllBlockGroups()
        deleteAllRules()
        if (rules.isNotEmpty()) {
            upsertRules(rules)
        }
        if (blockGroups.isNotEmpty()) {
            upsertBlockGroups(blockGroups)
        }
        if (blockGroupMembers.isNotEmpty()) {
            upsertBlockGroupMembers(blockGroupMembers)
        }
        if (countries.isNotEmpty()) {
            deleteAllCountryRules()
            upsertCountryRules(countries)
        }
        deleteAllEvents()
        deleteAllStatsEvents()
        if (events.isNotEmpty()) {
            insertEvents(events)
            insertStatsEvents(events.map { it.toStatsEvent() })
        }
    }
}

data class CountryStatRow(
    val countryIso: String?,
    val total: Int,
)

data class BlockGroupMatchRow(
    val groupId: Long,
    val groupName: String,
    val contactName: String,
    val phoneNumber: String,
)

private fun EventLogEntity.toStatsEvent() = StatsEventEntity(
    timestampMillis = timestampMillis,
    phoneNumber = phoneNumber,
    normalizedNumber = normalizedNumber,
    action = action,
    riskLevel = riskLevel,
    score = score,
    reason = reason,
    matchedRuleId = matchedRuleId,
    countryIso = countryIso,
)
