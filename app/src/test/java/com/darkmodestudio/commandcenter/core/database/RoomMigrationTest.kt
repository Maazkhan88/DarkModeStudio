package com.darkmodestudio.commandcenter.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = android.app.Application::class)
class RoomMigrationTest {

    private val TEST_DB = "migration_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DmsDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration4to5_usingMigrationTestHelper_validatesSchemaAndPreservesAllData() {
        // 1. Create canonical v4 database using exported historical 4.json schema
        val dbV4 = helper.createDatabase(TEST_DB, 4)

        // 2. Pre-populate realistic v4 data across all domains
        dbV4.execSQL(
            """
            INSERT INTO projects (
                id, name, description, iconTag, status, isMvp, owner, createdAt, dueDate, nextMilestone,
                manualProgressOverride, planningWeight, developmentWeight, testingWeight, deploymentWeight, lastUpdate, repositoryFullName
            ) VALUES (
                'secondme', 'SecondMe', 'AI Memory Agent', 'SM', 'IN_PROGRESS', 1, 'Maaz', '2026-08-30', '2026-09-01', 'Alpha Launch',
                0.65, 0.15, 0.45, 0.20, 0.20, 'Just now', 'Maazkhan88/SecondMe'
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO tasks (
                id, projectId, projectName, title, description, status, priority, assignedAgent, dueTime, createdAt, completedAt
            ) VALUES (
                't1', 'secondme', 'SecondMe', 'Auth Engine', 'Implement Keystore', 'DONE', 'HIGH', 'Codex', '12:00 PM', 'Today', '2026-08-30 12:00:00'
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO agents (
                id, name, provider, mode, speed, runsUsed, runsTotal, messagesUsed, messagesTotal, tasksUsed, tasksTotal, currentTask, statusText, usagePercentage
            ) VALUES (
                'codex', 'Codex', 'OPENAI', 'Pro', 'Fast', 92, 500, 2450, 5000, 18, 100, 'Refactoring AST', 'Active • 49%', 0.49
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO automation_rules (
                id, name, triggerType, providerId, projectId, actionType, isEnabled, humanReadableText
            ) VALUES (
                'rule1', 'GitHub Action Failure Alert', 'GITHUB_WORKFLOW_FAILED', 'github', NULL, 'SEND_NOTIFICATION', 0, 'Custom silenced CI alert'
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO integrations (
                id, name, category, isConnected, health, lastSync, lastSuccessfulSync, lastError, primaryMetric
            ) VALUES (
                'github', 'GitHub', 'Code & CI/CD', 1, 'OPERATIONAL', 'Just now', 'Just now', NULL, 'All CI Actions Passing'
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO integration_metrics (
                id, integrationId, label, value
            ) VALUES (
                1, 'github', 'Open PRs', '14 open PRs'
            )
            """.trimIndent()
        )

        dbV4.execSQL(
            """
            INSERT INTO repository_file_entries (
                id, repositoryFullName, path, name, fullPath, type, size, sha, downloadUrl, lastCached
            ) VALUES (
                'f1', 'Maazkhan88/SecondMe', '', 'README.md', 'README.md', 'file', 1024, 'sha1', NULL, 123456789
            )
            """.trimIndent()
        )

        dbV4.close()

        // 3. Run migration 4 -> 5 and validate against exported 5.json schema with MigrationTestHelper
        val dbV5 = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // 4. Verify Project data and repositoryDefaultBranch column
        val cursorProj = dbV5.query("SELECT * FROM projects WHERE id = 'secondme'")
        assertTrue(cursorProj.moveToFirst())
        assertEquals("SecondMe", cursorProj.getString(cursorProj.getColumnIndexOrThrow("name")))
        assertEquals("Maazkhan88/SecondMe", cursorProj.getString(cursorProj.getColumnIndexOrThrow("repositoryFullName")))
        assertNull("repositoryDefaultBranch should be null until synchronized", cursorProj.getString(cursorProj.getColumnIndexOrThrow("repositoryDefaultBranch")))
        assertEquals(0.65f, cursorProj.getFloat(cursorProj.getColumnIndexOrThrow("manualProgressOverride")), 0.001f)
        cursorProj.close()

        // 5. Verify Tasks data and composite index structure
        val cursorTask = dbV5.query("SELECT * FROM tasks WHERE id = 't1'")
        assertTrue(cursorTask.moveToFirst())
        assertEquals("Auth Engine", cursorTask.getString(cursorTask.getColumnIndexOrThrow("title")))
        assertEquals("Codex", cursorTask.getString(cursorTask.getColumnIndexOrThrow("assignedAgent")))
        assertEquals("DONE", cursorTask.getString(cursorTask.getColumnIndexOrThrow("status")))
        cursorTask.close()

        val cursorIdx = dbV5.query("PRAGMA index_list('tasks')")
        val indexes = mutableListOf<String>()
        while (cursorIdx.moveToNext()) {
            indexes.add(cursorIdx.getString(cursorIdx.getColumnIndexOrThrow("name")))
        }
        cursorIdx.close()
        assertTrue("Composite index index_tasks_projectId_status must exist", indexes.contains("index_tasks_projectId_status"))
        assertFalse("Old single-column index_tasks_projectId must be dropped", indexes.contains("index_tasks_projectId"))

        // 6. Verify Agent telemetry preserved
        val cursorAgent = dbV5.query("SELECT * FROM agents WHERE id = 'codex'")
        assertTrue(cursorAgent.moveToFirst())
        assertEquals(92, cursorAgent.getInt(cursorAgent.getColumnIndexOrThrow("runsUsed")))
        assertEquals(2450, cursorAgent.getInt(cursorAgent.getColumnIndexOrThrow("messagesUsed")))
        assertEquals(18, cursorAgent.getInt(cursorAgent.getColumnIndexOrThrow("tasksUsed")))
        assertEquals("Refactoring AST", cursorAgent.getString(cursorAgent.getColumnIndexOrThrow("currentTask")))
        cursorAgent.close()

        // 7. Verify Automation Rule user customizations preserved
        val cursorRule = dbV5.query("SELECT * FROM automation_rules WHERE id = 'rule1'")
        assertTrue(cursorRule.moveToFirst())
        assertEquals(0, cursorRule.getInt(cursorRule.getColumnIndexOrThrow("isEnabled")))
        assertEquals("Custom silenced CI alert", cursorRule.getString(cursorRule.getColumnIndexOrThrow("humanReadableText")))
        cursorRule.close()

        // 8. Verify Integration Metric preserved
        val cursorMetric = dbV5.query("SELECT * FROM integration_metrics WHERE label = 'Open PRs'")
        assertTrue(cursorMetric.moveToFirst())
        assertEquals("14 open PRs", cursorMetric.getString(cursorMetric.getColumnIndexOrThrow("value")))
        cursorMetric.close()

        // 9. Verify Repository File Entry migrated with branch = 'main'
        val cursorFile = dbV5.query("SELECT * FROM repository_file_entries WHERE name = 'README.md'")
        assertTrue(cursorFile.moveToFirst())
        assertEquals("main", cursorFile.getString(cursorFile.getColumnIndexOrThrow("branch")))
        assertEquals("Maazkhan88/SecondMe", cursorFile.getString(cursorFile.getColumnIndexOrThrow("repositoryFullName")))
        cursorFile.close()

        dbV5.close()
    }

    @Test
    fun migration5to6_usingMigrationTestHelper_validatesSchemaAndCreatesConnectionTables() {
        val dbV5 = helper.createDatabase(TEST_DB, 5)
        dbV5.execSQL(
            """
            INSERT INTO projects (
                id, name, description, iconTag, status, isMvp, owner, createdAt, dueDate, nextMilestone,
                manualProgressOverride, planningWeight, developmentWeight, testingWeight, deploymentWeight, lastUpdate,
                repositoryFullName, repositoryDefaultBranch
            ) VALUES (
                'app1', 'App 1', 'Desc', 'A1', 'IMPORTED', 0, 'User', '2026-08-31', '', '',
                NULL, 0.0, 0.0, 0.0, 0.0, 'Today', 'User/App1', 'main'
            )
            """.trimIndent()
        )
        dbV5.close()

        val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Verify project data is preserved
        val cursorProj = dbV6.query("SELECT * FROM projects WHERE id = 'app1'")
        assertTrue(cursorProj.moveToFirst())
        assertEquals("App 1", cursorProj.getString(cursorProj.getColumnIndexOrThrow("name")))
        cursorProj.close()

        // Verify provider_connections table exists and allows insertion
        dbV6.execSQL(
            """
            INSERT INTO provider_connections (
                providerId, authMethod, connectionState, accountDisplayName, lastVerifiedAt
            ) VALUES (
                'github', 'OAuth 2.0 (PKCE)', 'CONNECTED', 'Maazkhan88', 'Just now'
            )
            """.trimIndent()
        )
        val cursorConn = dbV6.query("SELECT * FROM provider_connections WHERE providerId = 'github'")
        assertTrue(cursorConn.moveToFirst())
        assertEquals("CONNECTED", cursorConn.getString(cursorConn.getColumnIndexOrThrow("connectionState")))
        cursorConn.close()

        dbV6.close()
    }

    @Test
    fun migration6to7_usingMigrationTestHelper_validatesSchemaAndRemovesAuthTokenSecret() {
        val dbV6 = helper.createDatabase(TEST_DB, 6)
        dbV6.execSQL(
            """
            INSERT INTO desktop_hosts (
                hostId, hostName, hostAddress, isOnline, lastSeen, authToken, availableAgents
            ) VALUES (
                'primary_desktop', 'Dev-Workstation', '192.168.1.50:8998', 1, 'Just now', 'old_secret_to_purge', 'codex,claude,antigravity'
            )
            """.trimIndent()
        )
        dbV6.close()

        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        val cursor = dbV7.query("SELECT * FROM desktop_hosts WHERE hostId = 'primary_desktop'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Dev-Workstation", cursor.getString(cursor.getColumnIndexOrThrow("hostName")))
        assertEquals("192.168.1.50:8998", cursor.getString(cursor.getColumnIndexOrThrow("hostAddress")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isOnline")))
        assertEquals("codex,claude,antigravity", cursor.getString(cursor.getColumnIndexOrThrow("availableAgents")))

        // Verify authToken column is removed
        assertEquals(-1, cursor.getColumnIndex("authToken"))
        // Verify credentialAlias column exists
        assertTrue(cursor.getColumnIndex("credentialAlias") >= 0)
        cursor.close()
        dbV7.close()
    }

    @Test
    fun migration4to7_fullChain_validatesSchemaAndPreservesAllData() {
        val dbV4 = helper.createDatabase(TEST_DB, 4)
        dbV4.execSQL(
            """
            INSERT INTO projects (
                id, name, description, iconTag, status, isMvp, owner, createdAt, dueDate, nextMilestone,
                manualProgressOverride, planningWeight, developmentWeight, testingWeight, deploymentWeight, lastUpdate, repositoryFullName
            ) VALUES (
                'secondme', 'SecondMe', 'AI Memory Agent', 'SM', 'IMPORTED', 0, 'Maaz', '2026-08-30', '', '',
                NULL, 0.0, 0.0, 0.0, 0.0, 'Just now', 'Maazkhan88/SecondMe'
            )
            """.trimIndent()
        )
        dbV4.close()

        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
        val cursor = dbV7.query("SELECT * FROM projects WHERE id = 'secondme'")
        assertTrue(cursor.moveToFirst())
        assertEquals("SecondMe", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.close()
        dbV7.close()
    }
}
