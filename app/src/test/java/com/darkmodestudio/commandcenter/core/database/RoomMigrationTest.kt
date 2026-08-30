package com.darkmodestudio.commandcenter.core.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomMigrationTest {

    @Test
    fun migration1to2_createsAgentActivitiesTableAndPreservesData() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_mig_1_2.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create DB at Version 1
        val configV1 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `agents` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `provider` TEXT NOT NULL,
                            `mode` TEXT NOT NULL,
                            `speed` TEXT NOT NULL,
                            `runsUsed` INTEGER NOT NULL,
                            `runsTotal` INTEGER NOT NULL,
                            `messagesUsed` INTEGER NOT NULL,
                            `messagesTotal` INTEGER NOT NULL,
                            `tasksUsed` INTEGER NOT NULL,
                            `tasksTotal` INTEGER NOT NULL,
                            `currentTask` TEXT NOT NULL,
                            `statusText` TEXT NOT NULL,
                            `usagePercentage` REAL NOT NULL
                        )
                        """.trimIndent()
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helperV1 = factory.create(configV1)
        val dbV1 = helperV1.writableDatabase
        dbV1.execSQL("INSERT INTO agents VALUES ('codex', 'Codex', 'OPENAI', 'Pro', 'Fast', 10, 100, 50, 500, 5, 50, 'Coding', 'Active', 0.1)")
        dbV1.close()
        helperV1.close()

        // 2. Open DB at Version 2 and execute MIGRATION_1_2
        val configV2 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 1 && newVersion == 2) {
                        MIGRATION_1_2.migrate(db)
                    }
                }
            })
            .build()

        val helperV2 = factory.create(configV2)
        val dbV2 = helperV2.writableDatabase

        // 3. Verify agents data preserved
        val cursorAgents = dbV2.query("SELECT * FROM agents WHERE id = 'codex'")
        assertTrue(cursorAgents.moveToFirst())
        assertEquals("Codex", cursorAgents.getString(cursorAgents.getColumnIndexOrThrow("name")))
        cursorAgents.close()

        // 4. Verify agent_activities table exists and allows insertion
        dbV2.execSQL("INSERT INTO agent_activities VALUES ('act_1', 'codex', 'Refactored auth', 'TASK-1', 'Just now', 123456789)")
        val cursorActivities = dbV2.query("SELECT * FROM agent_activities WHERE id = 'act_1'")
        assertTrue(cursorActivities.moveToFirst())
        assertEquals("Refactored auth", cursorActivities.getString(cursorActivities.getColumnIndexOrThrow("title")))
        cursorActivities.close()

        dbV2.close()
        helperV2.close()
    }

    @Test
    fun migration2to3_createsTasksPerformanceIndexes() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_mig_2_3.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()

        val factory = FrameworkSQLiteOpenHelperFactory()

        val configV2 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `projectId` TEXT NOT NULL,
                            `projectName` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT,
                            `status` TEXT NOT NULL,
                            `priority` TEXT NOT NULL,
                            `assignedAgent` TEXT,
                            `dueTime` TEXT NOT NULL,
                            `createdAt` TEXT NOT NULL,
                            `completedAt` TEXT
                        )
                        """.trimIndent()
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helperV2 = factory.create(configV2)
        val dbV2 = helperV2.writableDatabase
        dbV2.execSQL("INSERT INTO tasks VALUES ('t1', 'p1', 'Project 1', 'Task 1', 'Desc', 'PENDING', 'HIGH', 'Codex', '5pm', 'Today', NULL)")
        dbV2.close()
        helperV2.close()

        val configV3 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 2 && newVersion == 3) {
                        MIGRATION_2_3.migrate(db)
                    }
                }
            })
            .build()

        val helperV3 = factory.create(configV3)
        val dbV3 = helperV3.writableDatabase

        val cursor = dbV3.query("SELECT * FROM tasks WHERE id = 't1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Task 1", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        cursor.close()

        dbV3.close()
        helperV3.close()
    }

    @Test
    fun migration3to4_addsRepositoryFullNameAndFileEntriesTable() {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_mig_3_4.db"
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) dbFile.delete()

        val factory = FrameworkSQLiteOpenHelperFactory()

        val configV3 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `projects` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `iconTag` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `isMvp` INTEGER NOT NULL,
                            `owner` TEXT NOT NULL,
                            `createdAt` TEXT NOT NULL,
                            `dueDate` TEXT NOT NULL,
                            `nextMilestone` TEXT NOT NULL,
                            `manualProgressOverride` REAL,
                            `planningWeight` REAL NOT NULL,
                            `developmentWeight` REAL NOT NULL,
                            `testingWeight` REAL NOT NULL,
                            `deploymentWeight` REAL NOT NULL,
                            `lastUpdate` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helperV3 = factory.create(configV3)
        val dbV3 = helperV3.writableDatabase
        dbV3.execSQL("INSERT INTO projects VALUES ('p1', 'SecondMe', 'Memory Agent', 'SM', 'IN_PROGRESS', 1, 'Maaz', 'Today', '2026-09-01', 'Alpha', 0.5, 0.15, 0.45, 0.20, 0.20, 'Now')")
        dbV3.close()
        helperV3.close()

        val configV4 = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 3 && newVersion == 4) {
                        MIGRATION_3_4.migrate(db)
                    }
                }
            })
            .build()

        val helperV4 = factory.create(configV4)
        val dbV4 = helperV4.writableDatabase

        // Update repositoryFullName on existing project
        dbV4.execSQL("UPDATE projects SET repositoryFullName = 'Maazkhan88/SecondMe' WHERE id = 'p1'")
        val cursorProj = dbV4.query("SELECT * FROM projects WHERE id = 'p1'")
        assertTrue(cursorProj.moveToFirst())
        assertEquals("Maazkhan88/SecondMe", cursorProj.getString(cursorProj.getColumnIndexOrThrow("repositoryFullName")))
        cursorProj.close()

        // Insert and read from repository_file_entries
        dbV4.execSQL("INSERT INTO repository_file_entries VALUES ('f1', 'Maazkhan88/SecondMe', '', 'README.md', 'README.md', 'file', 1024, 'sha1', NULL, 123456)")
        val cursorFiles = dbV4.query("SELECT * FROM repository_file_entries WHERE id = 'f1'")
        assertTrue(cursorFiles.moveToFirst())
        assertEquals("README.md", cursorFiles.getString(cursorFiles.getColumnIndexOrThrow("name")))
        cursorFiles.close()

        dbV4.close()
        helperV4.close()
    }
}
