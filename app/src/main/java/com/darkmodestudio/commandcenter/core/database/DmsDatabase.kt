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
import com.darkmodestudio.commandcenter.core.database.dao.DesktopHostDao
import com.darkmodestudio.commandcenter.core.database.dao.IntegrationDao
import com.darkmodestudio.commandcenter.core.database.dao.NotificationDao
import com.darkmodestudio.commandcenter.core.database.dao.ProjectDao
import com.darkmodestudio.commandcenter.core.database.dao.ProviderConnectionDao
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
import com.darkmodestudio.commandcenter.core.database.entity.DesktopHostEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationIncidentEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationMetricEntity
import com.darkmodestudio.commandcenter.core.database.entity.NotificationEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectBlockerEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectMilestoneEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProviderConnectionEntity
import com.darkmodestudio.commandcenter.core.database.entity.ReminderEntity
import com.darkmodestudio.commandcenter.core.database.entity.RepositoryFileEntryEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import java.io.File

/**
 * Historical Migration 1 -> 2: Adds agent_activities table
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
 * Historical Migration 2 -> 3: Adds individual status & projectId indexes on tasks table
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")
    }
}

/**
 * Historical Migration 3 -> 4: Adds repositoryFullName to projects table & creates repository_file_entries table
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repositoryFullName` TEXT DEFAULT NULL")
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

/**
 * Canonical Migration 4 -> 5:
 * 1. Adds repositoryDefaultBranch to projects table
 * 2. Corrects tasks index: creates composite (projectId, status), drops redundant projectId-only index
 * 3. Migrates repository_file_entries to branch-scoped schema
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add repositoryDefaultBranch
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repositoryDefaultBranch` TEXT DEFAULT NULL")

        // 2. Correct tasks index structure
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId_status` ON `tasks` (`projectId`, `status`)")
        db.execSQL("DROP INDEX IF EXISTS `index_tasks_projectId`")

        // 3. Migrate repository_file_entries to branch-scoped schema
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `repository_file_entries_new` (
                `id` TEXT NOT NULL,
                `repositoryFullName` TEXT NOT NULL,
                `branch` TEXT NOT NULL DEFAULT 'main',
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
        db.execSQL(
            """
            INSERT INTO `repository_file_entries_new` (
                `id`, `repositoryFullName`, `branch`, `path`, `name`, `fullPath`, `type`, `size`, `sha`, `downloadUrl`, `lastCached`
            )
            SELECT 
                `repositoryFullName` || ':main:' || `path` || ':' || `name`,
                `repositoryFullName`,
                'main',
                `path`,
                `name`,
                `fullPath`,
                `type`,
                `size`,
                `sha`,
                `downloadUrl`,
                `lastCached`
            FROM `repository_file_entries`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS `repository_file_entries`")
        db.execSQL("ALTER TABLE `repository_file_entries_new` RENAME TO `repository_file_entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repository_file_entries_repositoryFullName_branch_path` ON `repository_file_entries` (`repositoryFullName`, `branch`, `path`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `provider_connections` (
                `providerId` TEXT NOT NULL,
                `authMethod` TEXT NOT NULL,
                `connectionState` TEXT NOT NULL,
                `accountDisplayName` TEXT,
                `accountId` TEXT,
                `workspaceName` TEXT,
                `grantedScopes` TEXT,
                `expiresAt` INTEGER,
                `lastVerifiedAt` TEXT,
                `lastError` TEXT,
                `runtimeHostId` TEXT,
                PRIMARY KEY(`providerId`)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `desktop_hosts` (
                `hostId` TEXT NOT NULL,
                `hostName` TEXT NOT NULL,
                `hostAddress` TEXT NOT NULL,
                `isOnline` INTEGER NOT NULL,
                `lastSeen` TEXT NOT NULL,
                `authToken` TEXT,
                `availableAgents` TEXT NOT NULL,
                PRIMARY KEY(`hostId`)
            )
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `desktop_hosts_new` (
                `hostId` TEXT NOT NULL,
                `hostName` TEXT NOT NULL,
                `hostAddress` TEXT NOT NULL,
                `isOnline` INTEGER NOT NULL,
                `lastSeen` TEXT NOT NULL,
                `availableAgents` TEXT NOT NULL,
                `credentialAlias` TEXT,
                PRIMARY KEY(`hostId`)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `desktop_hosts_new` (`hostId`, `hostName`, `hostAddress`, `isOnline`, `lastSeen`, `availableAgents`, `credentialAlias`)
            SELECT `hostId`, `hostName`, `hostAddress`, `isOnline`, `lastSeen`, `availableAgents`, NULL FROM `desktop_hosts`
        """.trimIndent())
        db.execSQL("DROP TABLE IF EXISTS `desktop_hosts`")
        db.execSQL("ALTER TABLE `desktop_hosts_new` RENAME TO `desktop_hosts`")
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
        RepositoryFileEntryEntity::class,
        ProviderConnectionEntity::class,
        DesktopHostEntity::class
    ],
    version = 7,
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
    abstract fun providerConnectionDao(): ProviderConnectionDao
    abstract fun desktopHostDao(): DesktopHostDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Crash-Safe All-or-Nothing Database Fileset Migration
         * Uses temporary staging files (.migrating) and promotes the final main DB filename LAST (the commit point).
         * Legacy files are only deleted after the final promoted fileset is verified.
         */
        fun migrateLegacyDatabaseFileIfPresent(context: Context): Boolean {
            val legacyDb = context.getDatabasePath(LEGACY_DATABASE_NAME)
            val currentDb = context.getDatabasePath(DATABASE_NAME)

            // If destination already exists or source does not exist, abort safely
            if (currentDb.exists() || !legacyDb.exists()) {
                return false
            }

            val parentDir = legacyDb.parentFile ?: return false
            val legacyWal = File(parentDir, "$LEGACY_DATABASE_NAME-wal")
            val legacyShm = File(parentDir, "$LEGACY_DATABASE_NAME-shm")

            val currentWal = File(parentDir, "$DATABASE_NAME-wal")
            val currentShm = File(parentDir, "$DATABASE_NAME-shm")

            val tempDb = File(parentDir, "$DATABASE_NAME.migrating")
            val tempWal = File(parentDir, "$DATABASE_NAME-wal.migrating")
            val tempShm = File(parentDir, "$DATABASE_NAME-shm.migrating")

            val hasWal = legacyWal.exists()
            val hasShm = legacyShm.exists()

            parentDir.mkdirs()

            // Clean up any stale incomplete temp migration files from a previous crashed attempt
            if (tempDb.exists()) tempDb.delete()
            if (tempWal.exists()) tempWal.delete()
            if (tempShm.exists()) tempShm.delete()

            return try {
                // Stage 1: Safe copy legacy primary database to temp staging
                legacyDb.copyTo(tempDb, overwrite = true)
                if (!tempDb.exists() || (legacyDb.length() > 0L && tempDb.length() != legacyDb.length())) {
                    tempDb.delete()
                    return false
                }

                // Stage 2: Safe copy legacy WAL to temp staging if present
                if (hasWal) {
                    legacyWal.copyTo(tempWal, overwrite = true)
                    if (!tempWal.exists() || (legacyWal.length() > 0L && tempWal.length() != legacyWal.length())) {
                        tempDb.delete()
                        tempWal.delete()
                        return false
                    }
                }

                // Stage 3: Safe copy legacy SHM to temp staging if present
                if (hasShm) {
                    legacyShm.copyTo(tempShm, overwrite = true)
                    if (!tempShm.exists() || (legacyShm.length() > 0L && tempShm.length() != legacyShm.length())) {
                        tempDb.delete()
                        tempWal.delete()
                        tempShm.delete()
                        return false
                    }
                }

                // Stage 4: Promote staging companions first
                if (hasWal) {
                    val walPromoted = tempWal.renameTo(currentWal)
                    if (!walPromoted || !currentWal.exists()) {
                        tempDb.delete()
                        tempWal.delete()
                        tempShm.delete()
                        currentWal.delete()
                        return false
                    }
                }

                if (hasShm) {
                    val shmPromoted = tempShm.renameTo(currentShm)
                    if (!shmPromoted || !currentShm.exists()) {
                        tempDb.delete()
                        tempShm.delete()
                        currentWal.delete()
                        currentShm.delete()
                        return false
                    }
                }

                // Stage 5: Promote temp main DB -> final main DB LAST (Commit Point)
                val mainPromoted = tempDb.renameTo(currentDb)
                if (!mainPromoted || !currentDb.exists()) {
                    tempDb.delete()
                    currentDb.delete()
                    currentWal.delete()
                    currentShm.delete()
                    return false
                }

                // Stage 6: Verify final fileset
                if (!currentDb.exists()) return false
                if (hasWal && !currentWal.exists()) return false
                if (hasShm && !currentShm.exists()) return false

                // Stage 7: Delete legacy source files now that final promotion is committed
                legacyDb.delete()
                if (hasWal) legacyWal.delete()
                if (hasShm) legacyShm.delete()

                true
            } catch (_: Exception) {
                // Ensure temp files and partial final files are cleaned up on exception
                if (tempDb.exists()) tempDb.delete()
                if (tempWal.exists()) tempWal.delete()
                if (tempShm.exists()) tempShm.delete()
                if (currentDb.exists()) currentDb.delete()
                if (currentWal.exists()) currentWal.delete()
                if (currentShm.exists()) currentShm.delete()
                false
            }
        }
    }
}
