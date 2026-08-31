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
    private lateinit var tempDbFile: File
    private lateinit var tempWalFile: File
    private lateinit var tempShmFile: File

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        legacyDbFile = context.getDatabasePath(DmsDatabase.LEGACY_DATABASE_NAME)
        currentDbFile = context.getDatabasePath(DmsDatabase.DATABASE_NAME)

        val parent = legacyDbFile.parentFile
        legacyWalFile = File(parent, "${DmsDatabase.LEGACY_DATABASE_NAME}-wal")
        legacyShmFile = File(parent, "${DmsDatabase.LEGACY_DATABASE_NAME}-shm")

        currentWalFile = File(parent, "${DmsDatabase.DATABASE_NAME}-wal")
        currentShmFile = File(parent, "${DmsDatabase.DATABASE_NAME}-shm")

        tempDbFile = File(parent, "${DmsDatabase.DATABASE_NAME}.migrating")
        tempWalFile = File(parent, "${DmsDatabase.DATABASE_NAME}-wal.migrating")
        tempShmFile = File(parent, "${DmsDatabase.DATABASE_NAME}-shm.migrating")

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
        tempDbFile.delete()
        tempWalFile.delete()
        tempShmFile.delete()
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
    fun caseC_whenNoLegacyDbExists_returnsFalseWithoutTouchingAnything() {
        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertFalse(result)
        assertFalse(currentDbFile.exists())
    }

    @Test
    fun caseD_staleTempFilesFromPreviousCrash_cleanedUpAndMigrationSucceeds() {
        legacyDbFile.parentFile?.mkdirs()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
        sqlite.execSQL("CREATE TABLE users (name TEXT)")
        sqlite.execSQL("INSERT INTO users VALUES ('Alice')")
        sqlite.close()

        // Create corrupt/stale temporary files simulating a mid-flight process termination
        tempDbFile.writeText("corrupted stale temp db")
        tempWalFile.writeText("corrupted stale temp wal")

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue("Migration should recover from stale temp files", result)
        assertFalse("Stale temp DB must be cleaned up", tempDbFile.exists())
        assertFalse("Stale temp WAL must be cleaned up", tempWalFile.exists())
        assertTrue("Final current DB must exist", currentDbFile.exists())

        val targetSqlite = SQLiteDatabase.openDatabase(currentDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = targetSqlite.rawQuery("SELECT name FROM users", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("Alice", cursor.getString(0))
        cursor.close()
        targetSqlite.close()
    }

    @Test
    fun caseE_simulatedCrashBeforeFinalMainPromotion_recoversCompletelyOnNextAttempt() {
        legacyDbFile.parentFile?.mkdirs()

        val sqlite = SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
        sqlite.execSQL("CREATE TABLE settings (key TEXT, val TEXT)")
        sqlite.execSQL("INSERT INTO settings VALUES ('theme', 'dark')")
        sqlite.close()

        legacyWalFile.writeText("valid_wal_data")

        // Simulate crash right before tempDb.renameTo(currentDb) occurred:
        // tempDb and currentWal exist, but currentDb does NOT exist.
        tempDbFile.writeText("partial_temp")
        currentWalFile.writeText("orphaned_current_wal")

        // Next application startup calls migrateLegacyDatabaseFileIfPresent
        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue(result)
        assertTrue(currentDbFile.exists())
        assertTrue(currentWalFile.exists())
        assertFalse(tempDbFile.exists())
        assertFalse(legacyDbFile.exists())

        val targetSqlite = SQLiteDatabase.openDatabase(currentDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = targetSqlite.rawQuery("SELECT val FROM settings WHERE key = 'theme'", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("dark", cursor.getString(0))
        cursor.close()
        targetSqlite.close()
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
    fun caseG_realSqliteWalMode_preservesTransactionsThroughStagedMigration() {
        legacyDbFile.parentFile?.mkdirs()

        // Create SQLite database in WAL journal mode
        val sqlite = SQLiteDatabase.openOrCreateDatabase(legacyDbFile, null)
        sqlite.rawQuery("PRAGMA journal_mode=WAL", null).close()
        sqlite.execSQL("CREATE TABLE journal_test (id INTEGER PRIMARY KEY, msg TEXT)")
        sqlite.execSQL("INSERT INTO journal_test VALUES (1, 'WAL committed transaction')")
        sqlite.close()

        val result = DmsDatabase.migrateLegacyDatabaseFileIfPresent(context)

        assertTrue("WAL-mode migration must succeed", result)
        assertTrue("Destination DB must exist", currentDbFile.exists())
        assertFalse("Source DB must be deleted", legacyDbFile.exists())

        val targetSqlite = SQLiteDatabase.openDatabase(currentDbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        val cursor = targetSqlite.rawQuery("SELECT msg FROM journal_test WHERE id = 1", null)
        assertTrue(cursor.moveToFirst())
        assertEquals("WAL committed transaction", cursor.getString(0))
        cursor.close()
        targetSqlite.close()
    }
}
