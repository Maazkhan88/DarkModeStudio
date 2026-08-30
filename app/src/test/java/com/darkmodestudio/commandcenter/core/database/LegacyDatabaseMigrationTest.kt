package com.darkmodestudio.commandcenter.core.database

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LegacyDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var legacyDbFile: File
    private lateinit var currentDbFile: File
    private lateinit var legacyWalFile: File
    private lateinit var legacyShmFile: File
    private lateinit var currentWalFile: File
    private lateinit var currentShmFile: File

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        legacyDbFile = context.getDatabasePath(DmsDatabase.LEGACY_DATABASE_NAME)
        currentDbFile = context.getDatabasePath(DmsDatabase.DATABASE_NAME)

        legacyWalFile = File(legacyDbFile.parentFile, "${DmsDatabase.LEGACY_DATABASE_NAME}-wal")
        legacyShmFile = File(legacyDbFile.parentFile, "${DmsDatabase.LEGACY_DATABASE_NAME}-shm")

        currentWalFile = File(currentDbFile.parentFile, "${DmsDatabase.DATABASE_NAME}-wal")
        currentShmFile = File(currentDbFile.parentFile, "${DmsDatabase.DATABASE_NAME}-shm")

        cleanupFiles()
    }

    @After
    fun teardown() {
        cleanupFiles()
    }

    private fun cleanupFiles() {
        legacyDbFile.delete()
        legacyWalFile.delete()
        legacyShmFile.delete()
        currentDbFile.delete()
        currentWalFile.delete()
        currentShmFile.delete()
    }

    @Test
    fun migration_whenLegacyDbPresent_movesAllDbFilesSuccessfully() {
        legacyDbFile.parentFile?.mkdirs()
        legacyDbFile.writeText("SQLite format 3 legacy db data")
        legacyWalFile.writeText("legacy wal journal")
        legacyShmFile.writeText("legacy shm shared memory")

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue(result)
        assertFalse("Legacy DB should no longer exist", legacyDbFile.exists())
        assertFalse("Legacy WAL should no longer exist", legacyWalFile.exists())
        assertFalse("Legacy SHM should no longer exist", legacyShmFile.exists())

        assertTrue("Current DB must exist", currentDbFile.exists())
        assertTrue("Current WAL must exist", currentWalFile.exists())
        assertTrue("Current SHM must exist", currentShmFile.exists())

        assertEquals("SQLite format 3 legacy db data", currentDbFile.readText())
        assertEquals("legacy wal journal", currentWalFile.readText())
        assertEquals("legacy shm shared memory", currentShmFile.readText())
    }

    @Test
    fun migration_whenCurrentDbAlreadyExists_abortsMigrationSafely() {
        legacyDbFile.parentFile?.mkdirs()
        legacyDbFile.writeText("old legacy")
        currentDbFile.writeText("active current")

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertFalse(result)
        assertTrue(legacyDbFile.exists())
        assertTrue(currentDbFile.exists())
        assertEquals("active current", currentDbFile.readText())
    }

    @Test
    fun migration_whenNoLegacyDbExists_returnsFalseWithoutTouchingAnything() {
        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertFalse(result)
        assertFalse(currentDbFile.exists())
    }
}
