package com.callguardian.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        RuleEntity::class,
        BlockGroupEntity::class,
        BlockGroupMemberEntity::class,
        EventLogEntity::class,
        StatsEventEntity::class,
        CountryRuleEntity::class,
        AppSettingsEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CallGuardianDatabase : RoomDatabase() {
    abstract fun dao(): CallGuardianDao
}
