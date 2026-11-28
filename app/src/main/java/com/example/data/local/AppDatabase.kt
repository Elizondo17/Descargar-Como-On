package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BusinessInviteEntity
import com.example.data.model.BusinessProfile
import com.example.data.model.CallReminderEntity
import com.example.data.model.DeliveryLog
import com.example.data.model.PendingAgendaSyncEntity
import com.example.data.model.ReadNotificationEntity
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
        PendingAgendaSyncEntity::class,
        CallReminderEntity::class,
        ReadNotificationEntity::class,
        BusinessInviteEntity::class
    ],
    version = 20,
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
    abstract fun callReminderDao(): CallReminderDao
    abstract fun readNotificationDao(): ReadNotificationDao
    abstract fun businessInviteDao(): BusinessInviteDao
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

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `psa_call_reminders` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `triggerAtMs` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `isAutoPeakRecommendation` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `read_notification_ids` (
                        `notifId` TEXT NOT NULL PRIMARY KEY,
                        `readAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                var hasColumn = false
                db.query("PRAGMA table_info(`users`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0 && cursor.getString(nameIndex) == "tutorialCompleted") {
                            hasColumn = true
                            break
                        }
                    }
                }
                if (!hasColumn) {
                    db.execSQL(
                        "ALTER TABLE `users` ADD COLUMN `tutorialCompleted` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `business_invites` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `ownerUid` TEXT NOT NULL,
                        `sellerEmail` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `presentationDate` TEXT NOT NULL,
                        `presentationTime` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderAtMs` INTEGER NOT NULL,
                        `whatsappSentCount` INTEGER NOT NULL,
                        `linkedRequestId` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existingCols = mutableSetOf<String>()
                db.query("PRAGMA table_info(`users`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        if (nameIndex >= 0) {
                            existingCols.add(cursor.getString(nameIndex))
                        }
                    }
                }
                if ("fcmToken" !in existingCols) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `fcmToken` TEXT NOT NULL DEFAULT ''")
                }
                if ("fcmUpdatedAtMs" !in existingCols) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `fcmUpdatedAtMs` INTEGER NOT NULL DEFAULT 0")
                }
                if ("notifyCatalogUpdates" !in existingCols) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `notifyCatalogUpdates` INTEGER NOT NULL DEFAULT 1")
                }
                if ("notifyAssignedContacts" !in existingCols) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `notifyAssignedContacts` INTEGER NOT NULL DEFAULT 1")
                }
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
                    .addMigrations(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

