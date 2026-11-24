package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BusinessProfile
import com.example.data.model.DeliveryLog
import com.example.data.model.PendingAgendaSyncEntity
import com.example.data.model.ScriptFolder
import com.example.data.model.ScriptItem
import com.example.data.model.ScannedChat
import com.example.data.model.UserAccount
import com.example.data.model.UserToken

@Database(
    entities = [
        UserAccount::class,
        UserToken::class,
        ScriptFolder::class,
        ScriptItem::class,
        DeliveryLog::class,
        ScannedChat::class,
        BusinessProfile::class,
        com.example.data.model.BusinessPreset::class,
        com.example.data.model.SalesKnowledgeItem::class,
        com.example.data.model.PsaHomeVisit::class,
        com.example.data.model.PsaContactLead::class,
        com.example.data.model.PsaContactInteraction::class,
        com.example.data.model.PsaPostSaleInstallation::class,
        PendingAgendaSyncEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userTokenDao(): UserTokenDao
    abstract fun scriptDao(): ScriptDao
    abstract fun deliveryLogDao(): DeliveryLogDao
    abstract fun scannedChatDao(): ScannedChatDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun businessPresetDao(): BusinessPresetDao
    abstract fun salesKnowledgeDao(): SalesKnowledgeDao
    abstract fun psaVisitDao(): PsaVisitDao
    abstract fun psaContactDao(): PsaContactDao
    abstract fun psaPostSaleDao(): PsaPostSaleDao
    abstract fun agendaSyncDao(): AgendaSyncDao
    fun taskLogDao(): DeliveryLogDao = deliveryLogDao()

    companion object {
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_agenda_sync` (
                        `queueKey` TEXT NOT NULL PRIMARY KEY,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `ownerUid` TEXT NOT NULL,
                        `queuedAtMs` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ezocrm_database.db"
                )
                    .addMigrations(MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

