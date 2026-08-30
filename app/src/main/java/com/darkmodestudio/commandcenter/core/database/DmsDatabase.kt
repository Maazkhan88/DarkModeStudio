package com.darkmodestudio.commandcenter.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.darkmodestudio.commandcenter.core.database.dao.AgentDao
import com.darkmodestudio.commandcenter.core.database.dao.AutomationDao
import com.darkmodestudio.commandcenter.core.database.dao.IntegrationDao
import com.darkmodestudio.commandcenter.core.database.dao.NotificationDao
import com.darkmodestudio.commandcenter.core.database.dao.ProjectDao
import com.darkmodestudio.commandcenter.core.database.dao.ReminderDao
import com.darkmodestudio.commandcenter.core.database.dao.RepositoryFileDao
import com.darkmodestudio.commandcenter.core.database.dao.SettingsDao
import com.darkmodestudio.commandcenter.core.database.dao.TaskDao
import com.darkmodestudio.commandcenter.core.database.entity.AgentActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationExecutionEntity
import com.darkmodestudio.commandcenter.core.database.entity.AutomationRuleEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectBlockerEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectMilestoneEntity
import com.darkmodestudio.commandcenter.core.database.entity.ReminderEntity
import com.darkmodestudio.commandcenter.core.database.entity.RepositoryFileEntryEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import java.io.File

/**
 * Migration 1 -> 2: Adds agent_activities table
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `agent_activities` (
                `id` TEXT NOT NULL,
                `agentId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `taskReference` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`agentId`) REFERENCES `agents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_activities_agentId` ON `agent_activities` (`agentId`)")
    }
}

/**
 * Migration 2 -> 3: Adds performance indexes on tasks table (composite projectId + status, and status)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId_status` ON `tasks` (`projectId`, `status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
    }
}

/**
 * Migration 3 -> 4: Adds repositoryFullName, repositoryDefaultBranch to projects table & creates repository_file_entries table
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repositoryFullName` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repositoryDefaultBranch` TEXT DEFAULT NULL")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repository_file_entries` (
                `id` TEXT NOT NULL,
                `repositoryFullName` TEXT NOT NULL,
                `path` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `fullPath` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `size` INTEGER NOT NULL,
                `sha` TEXT NOT NULL,
                `downloadUrl` TEXT,
                `lastCached` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repository_file_entries_repositoryFullName_path` ON `repository_file_entries` (`repositoryFullName`, `path`)")
    }
}

@Database(
    entities = [
        ProjectEntity::class,
        ProjectMilestoneEntity::class,
        ProjectBlockerEntity::class,
        ProjectActivityEntity::class,
        TaskEntity::class,
        AgentEntity::class,
        AgentUsageSnapshotEntity::class,
        AgentActivityEntity::class,
        IntegrationEntity::class,
        IntegrationMetricEntity::class,
        IntegrationIncidentEntity::class,
        NotificationEntity::class,
        ReminderEntity::class,
        AutomationRuleEntity::class,
        AutomationExecutionEntity::class,
        AppSettingsEntity::class,
        RepositoryFileEntryEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DmsDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun agentDao(): AgentDao
    abstract fun integrationDao(): IntegrationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun automationDao(): AutomationDao
    abstract fun settingsDao(): SettingsDao
    abstract fun repositoryFileDao(): RepositoryFileDao

    companion object {
        const val DATABASE_NAME = "darkmodestudio_command_center.db"
        const val LEGACY_DATABASE_NAME = "dark_mode_studio.db"

        @Volatile
        private var INSTANCE: DmsDatabase? = null

        fun getInstance(context: Context): DmsDatabase {
            return INSTANCE ?: synchronized(this) {
                migrateLegacyDatabaseFileIfPresent(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DmsDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun migrateLegacyDatabaseFileIfPresent(context: Context): Boolean {
            val legacyDb = context.getDatabasePath(LEGACY_DATABASE_NAME)
            val currentDb = context.getDatabasePath(DATABASE_NAME)

            if (!legacyDb.exists() || currentDb.exists()) {
                return false
            }

            return try {
                legacyDb.parentFile?.mkdirs()
                val mainMoved = legacyDb.renameTo(currentDb)
                if (!mainMoved || !currentDb.exists()) {
                    return false
                }

                val legacyWal = File(legacyDb.parentFile, "$LEGACY_DATABASE_NAME-wal")
                val currentWal = File(currentDb.parentFile, "$DATABASE_NAME-wal")
                if (legacyWal.exists()) {
                    legacyWal.renameTo(currentWal)
                }

                val legacyShm = File(legacyDb.parentFile, "$LEGACY_DATABASE_NAME-shm")
                val currentShm = File(currentDb.parentFile, "$DATABASE_NAME-shm")
                if (legacyShm.exists()) {
                    legacyShm.renameTo(currentShm)
                }

                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
