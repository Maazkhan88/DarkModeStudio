# DARK MODE STUDIO — v1.6.4 CODEX REVIEW PACKAGE

**Date**: 2026-08-31  
**Target Version**: v1.6.4 (`versionCode = 3`, `versionName = "1.6.4"`)  
**Remediation Role**: Antigravity (QA Engineer + Secondary Developer)  
**Lead Reviewer**: Codex (Lead Architect)  
**Status**: READY FOR CODEX FINAL REVIEW  

---

## 1. Executive Summary & Verification Matrix

This remediation resolves the Room schema version lineage and final release correctness requirements:

| Remediation Item | Codex Requirement | Status | Verification & Evidence |
|---|---|---|---|
| **Room Schema Lineage** | Treat historical v4 schema as immutable history; bump current DB to v5 | **RESOLVED** | Restored canonical historical `4.json` (`81b343b1d29f50ae51d109db735fd323`). Exported new `5.json` (`a00c511a492ec6dfe365e990325a57e6`). |
| **MIGRATION_4_5** | Transform historical v4 to v5 without data loss | **RESOLVED** | Adds `projects.repositoryDefaultBranch`, creates composite `tasks(projectId, status)` index, drops redundant `index_tasks_projectId`, migrates `repository_file_entries` to branch-scoped schema. |
| **Real Migration Testing** | Use `MigrationTestHelper` & `runMigrationsAndValidate` | **RESOLVED** | `RoomMigrationTest.kt` executes `helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)` asserting schema validity and exact domain data retention across projects, tasks, agents, rules, integrations, and files. |
| **Legacy Database Fileset Migration** | All-or-safe atomic copy-and-verify migration | **RESOLVED** | `migrateLegacyDatabaseFileIfPresent()` in `DmsDatabase.kt` copies and validates primary DB, WAL, and SHM before deleting source files. Tested in `LegacyDatabaseMigrationTest.kt` with real SQLite verification. |
| **GitHub Project Planning State** | Imported repository $\neq$ In-Progress project | **RESOLVED** | Added `ProjectStatus.IMPORTED` (`"Imported"`). Newly discovered repos default to `IMPORTED` with `0f` phase weights and empty milestones. Preserves existing local planning state. |
| **Branch-Scoped File Cache** | Prevent cross-branch file collisions | **RESOLVED** | Added `branch` to `RepositoryFileEntryEntity`, `RepositoryFileDao`, and `RepositoryFilesRepository`. Tested independent coexistence of `main` and `develop` in `RepositoryFilesRepositoryTest.kt`. |
| **Release Version Metadata** | Bump app release version | **RESOLVED** | `app/build.gradle.kts`: `versionCode = 3`, `versionName = "1.6.4"`. `AppSettingsEntity.schemaSeedVersion = 5`. |
| **GitHub Actions CI** | Automated verification workflow | **RESOLVED** | Added `.github/workflows/android-verify.yml` running `testDebugUnitTest`, `lintDebug`, and `assembleDebug`. |

---

## 2. Room Schema Lineage & Migration Details

### Canonical Historical v4
- **Schema File**: `app/schemas/com.darkmodestudio.commandcenter.core.database.DmsDatabase/4.json`
- **Identity Hash**: `81b343b1d29f50ae51d109db735fd323`
- **Lineage**:
  - `projects`: includes `repositoryFullName TEXT`, does **not** include `repositoryDefaultBranch`.
  - `tasks`: indices `index_tasks_projectId` and `index_tasks_status`.
  - `repository_file_entries`: index `index_repository_file_entries_repositoryFullName_path`.

### Current Production v5
- **Schema File**: `app/schemas/com.darkmodestudio.commandcenter.core.database.DmsDatabase/5.json`
- **Identity Hash**: `a00c511a492ec6dfe365e990325a57e6`
- **Lineage**:
  - `projects`: added `repositoryDefaultBranch TEXT DEFAULT NULL`.
  - `tasks`: index `index_tasks_projectId_status` on `(projectId, status)`, `index_tasks_status` on `(status)`. Old `index_tasks_projectId` removed.
  - `repository_file_entries`: added `branch TEXT NOT NULL DEFAULT 'main'`, index `index_repository_file_entries_repositoryFullName_branch_path` on `(repositoryFullName, branch, path)`.

### Migration Sequence
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repositoryDefaultBranch` TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId_status` ON `tasks` (`projectId`, `status`)")
        db.execSQL("DROP INDEX IF EXISTS `index_tasks_projectId`")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `repository_file_entries_new` (
                `id` TEXT NOT NULL, `repositoryFullName` TEXT NOT NULL, `branch` TEXT NOT NULL DEFAULT 'main',
                `path` TEXT NOT NULL, `name` TEXT NOT NULL, `fullPath` TEXT NOT NULL, `type` TEXT NOT NULL,
                `size` INTEGER NOT NULL, `sha` TEXT NOT NULL, `downloadUrl` TEXT, `lastCached` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `repository_file_entries_new` SELECT `repositoryFullName` || ':main:' || `path` || ':' || `name`, `repositoryFullName`, 'main', `path`, `name`, `fullPath`, `type`, `size`, `sha`, `downloadUrl`, `lastCached` FROM `repository_file_entries`
        """.trimIndent())
        db.execSQL("DROP TABLE IF EXISTS `repository_file_entries`")
        db.execSQL("ALTER TABLE `repository_file_entries_new` RENAME TO `repository_file_entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_repository_file_entries_repositoryFullName_branch_path` ON `repository_file_entries` (`repositoryFullName`, `branch`, `path`)")
    }
}
```

---

## 3. Automated Verification Results

- **Unit & Migration Tests**: **39 passed, 0 failed, 0 ignored** (`./gradlew testDebugUnitTest`)
- **Android Lint**: **0 errors** (`./gradlew lintDebug`)
- **Debug APK Assembly**: **BUILD SUCCESSFUL** (`./gradlew assembleDebug`)

### Test Suite Execution
1. `RoomMigrationTest.kt` — MigrationTestHelper v4 $\rightarrow$ v5 schema validation & domain persistence.
2. `LegacyDatabaseMigrationTest.kt` — Fileset copy-and-verify migration (Cases A, B, C, F) with SQLite query verification.
3. `RepositoryFilesRepositoryTest.kt` — `NotLinked` state, `develop` non-main default branch parameter passing, and `main`/`develop` branch cache isolation without collision.
4. `SeedSyncRaceTest.kt` — Deterministic structural initialization, agent telemetry preservation, and automation rule customization preservation.
5. `GitHubPartialSuccessTest.kt` — Preserves last-known-good Open PRs and Workflows on HTTP 500 subrequest failures; maps newly discovered repos to `ProjectStatus.IMPORTED` with `0f` phase weights.
6. `RepositoriesTest.kt` — Project, task, agent, integration, notification, and settings DAO operations.
7. `GitHubConnectorTest.kt` — GitHub HTTP API connector serialization.
8. `GitHubConnectorErrorTest.kt` — Rate limiting, 401 auth failures, 500 server errors, network exceptions.
9. `GitHub304Test.kt` — HTTP 304 Not Modified caching and local state preservation.
10. `StackConnectorsTest.kt` — Cloudflare, Supabase, Vercel connectors.
11. `DmsTimeFormatterTest.kt` — ISO-8601 parsing, local date/time formatting, UTC edge cases.

---

## 4. GitHub Planning vs Local Planning Separation

- Newly imported repositories receive `ProjectStatus.IMPORTED`, `planningWeight = 0f`, `developmentWeight = 0f`, `testingWeight = 0f`, `deploymentWeight = 0f`, empty milestones.
- In `ProjectDetailScreen`, unconfigured imported repositories display `"Imported repository — Planning not configured"` and `"No milestones defined"` instead of synthetic 15/45/20/20 progress bars.
- Existing local projects preserve user-selected status, due date, milestones, manual progress override, phase weights, MVP flag, and assigned agent mappings during GitHub sync.
