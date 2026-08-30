package com.darkmodestudio.commandcenter.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
    fun caseA_fullFilesetMigration_withWalAndShm_migratesSuccessfullyAndRemainsReadable() {
        legacyDbFile.parentFile?.mkdirs()

        // Create a real SQLite database
        val sqlite = SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
        sqlite.execSQL("CREATE TABLE sample_data (id TEXT PRIMARY KEY, value TEXT)")
        sqlite.execSQL("INSERT INTO sample_data VALUES ('item_1', 'Verified Persistence')")
        sqlite.close()

        // Create WAL and SHM companions
        legacyWalFile.writeText("wal_companion_binary_stream")
        legacyShmFile.writeText("shm_companion_binary_stream")

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue("Migration must return true when all files migrate cleanly", result)
        assertFalse("Legacy main DB must be removed", legacyDbFile.exists())
        assertFalse("Legacy WAL must be removed", legacyWalFile.exists())
        assertFalse("Legacy SHM must be removed", legacyShmFile.exists())

        assertTrue("Target DB must exist", currentDbFile.exists())
        assertTrue("Target WAL must exist", currentWalFile.exists())
        assertTrue("Target SHM must exist", currentShmFile.exists())

        // Validate readability with real SQLite engine
        val targetSqlite = SQLiteDatabase.openDatabase(currentDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = targetSqlite.rawQuery("SELECT value FROM sample_data WHERE id = 'item_1'", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("Verified Persistence", cursor.getString(0))
        cursor.close()
        targetSqlite.close()
    }

    @Test
    fun caseB_targetDbAlreadyExists_abortsMigrationAndPreservesLegacyUntouched() {
        legacyDbFile.parentFile?.mkdirs()
        legacyDbFile.writeText("legacy content")
        legacyWalFile.writeText("legacy wal")
        currentDbFile.writeText("existing current database")

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertFalse("Migration must return false if destination already exists", result)
        assertTrue("Legacy DB must remain untouched", legacyDbFile.exists())
        assertTrue("Legacy WAL must remain untouched", legacyWalFile.exists())
        assertEquals("existing current database", currentDbFile.readText())
    }

    @Test
    fun caseF_standaloneLegacyDb_withoutWalOrShm_migratesSuccessfully() {
        legacyDbFile.parentFile?.mkdirs()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
        sqlite.execSQL("CREATE TABLE notes (text TEXT)")
        sqlite.execSQL("INSERT INTO notes VALUES ('Standalone DB migrated')")
        sqlite.close()

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue(result)
        assertFalse(legacyDbFile.exists())
        assertTrue(currentDbFile.exists())

        val targetSqlite = SQLiteDatabase.openDatabase(currentDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = targetSqlite.rawQuery("SELECT text FROM notes", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("Standalone DB migrated", cursor.getString(0))
        cursor.close()
        targetSqlite.close()
    }

    @Test
    fun caseC_whenNoLegacyDbExists_returnsFalseWithoutTouchingAnything() {
        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertFalse(result)
        assertFalse(currentDbFile.exists())
    }
}
