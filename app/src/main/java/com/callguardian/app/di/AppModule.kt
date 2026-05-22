package com.callguardian.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.callguardian.app.data.local.CallGuardianDao
import com.callguardian.app.data.local.CallGuardianDatabase
import com.callguardian.app.data.local.SecureDatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.google.gson.Gson
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: SecureDatabaseKeyProvider,
    ): CallGuardianDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(keyProvider.getOrCreatePassphrase())
        return Room.databaseBuilder(context, CallGuardianDatabase::class.java, "callguardian.db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideDao(database: CallGuardianDatabase): CallGuardianDao = database.dao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `stats_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestampMillis` INTEGER NOT NULL,
                    `phoneNumber` TEXT NOT NULL,
                    `normalizedNumber` TEXT NOT NULL,
                    `action` TEXT NOT NULL,
                    `riskLevel` TEXT NOT NULL,
                    `score` INTEGER NOT NULL,
                    `reason` TEXT NOT NULL,
                    `matchedRuleId` INTEGER,
                    `countryIso` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `stats_events` (
                    `timestampMillis`, `phoneNumber`, `normalizedNumber`, `action`, `riskLevel`,
                    `score`, `reason`, `matchedRuleId`, `countryIso`
                )
                SELECT
                    `timestampMillis`, `phoneNumber`, `normalizedNumber`, `action`, `riskLevel`,
                    `score`, `reason`, `matchedRuleId`, `countryIso`
                FROM `event_logs`
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `event_logs` ADD COLUMN `contactName` TEXT")
        }
    }
}
