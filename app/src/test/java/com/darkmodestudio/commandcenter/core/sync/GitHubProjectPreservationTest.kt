package com.darkmodestudio.commandcenter.core.sync

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.ProjectActivityEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectBlockerEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectMilestoneEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import com.darkmodestudio.commandcenter.core.network.GitHubConnector
import com.darkmodestudio.commandcenter.core.security.KeystoreCredentialManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GitHubProjectPreservationTest {

    private lateinit var database: DmsDatabase
    private lateinit var keystoreManager: KeystoreCredentialManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keystoreManager = KeystoreCredentialManager(context)
        keystoreManager.saveSecret("token_github", "test_mock_token")
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun gitHubSync_onExistingProject_preservesAllChildMilestonesBlockersActivitiesTasksAndPlanningState() = runBlocking {
        // 1. Setup existing local DMS project with user planning customizations
        val existingProject = ProjectEntity(
            id = "secondme",
            name = "SecondMe",
            description = "Original Local Description",
            iconTag = "SM",
            status = ProjectStatus.ON_TRACK,
            isMvp = true,
            owner = "Maazkhan88",
            createdAt = "2026-08-01",
            dueDate = "2026-09-15",
            nextMilestone = "Private Beta Launch",
            manualProgressOverride = 0.63f,
            planningWeight = 0.20f,
            developmentWeight = 0.40f,
            testingWeight = 0.20f,
            deploymentWeight = 0.20f,
            lastUpdate = "2 hours ago",
            repositoryFullName = "Maazkhan88/SecondMe",
            repositoryDefaultBranch = "main"
        )
        database.projectDao().insertProject(existingProject)

        // 2. Insert child relational records (FK CASCADE target rows)
        val milestone = ProjectMilestoneEntity(
            id = "m_beta",
            projectId = "secondme",
            title = "Private Beta",
            isCompleted = false,
            isActive = true,
            date = "2026-09-15",
            sortOrder = 1
        )
        database.projectDao().insertMilestones(listOf(milestone))

        val blocker = ProjectBlockerEntity(
            id = "b_apple",
            projectId = "secondme",
            description = "Awaiting Apple entitlement",
            severity = "High",
            duration = "2 days"
        )
        database.projectDao().insertBlockers(listOf(blocker))

        val localActivity = ProjectActivityEntity(
            id = "act_roadmap_change",
            projectId = "secondme",
            title = "User changed roadmap",
            author = "Product Owner",
            timestamp = "Yesterday"
        )
        database.projectDao().insertActivities(listOf(localActivity))

        val task = TaskEntity(
            id = "task_onboarding",
            projectId = "secondme",
            projectName = "SecondMe",
            title = "Prepare onboarding",
            description = "Design onboarding flow for beta users",
            status = TaskStatus.PENDING,
            priority = TaskPriority.HIGH,
            assignedAgent = "Codex",
            dueTime = "Tomorrow",
            createdAt = "Yesterday"
        )
        database.taskDao().insertTask(task)

        // Verify pre-sync counts
        assertEquals(1, database.projectDao().getMilestoneCount("secondme"))
        assertEquals(1, database.projectDao().getBlockerCount("secondme"))
        assertEquals(1, database.projectDao().getActivityCount("secondme"))
        assertEquals(1, database.taskDao().getTasksByProjectFlow("secondme").first().size)

        // 3. Mock GitHub API response for SecondMe repository with new commit
        val mockReposJson = """
            [
                {
                    "id": 201,
                    "name": "SecondMe",
                    "full_name": "Maazkhan88/SecondMe",
                    "private": false,
                    "description": "Updated Remote GitHub Description",
                    "default_branch": "develop",
                    "created_at": "2026-08-01T10:00:00Z",
                    "pushed_at": "2026-08-31T14:00:00Z"
                }
            ]
        """.trimIndent()

        val mockCommitsJson = """
            [
                {
                    "sha": "abcdef1234567890",
                    "commit": {
                        "message": "feat(auth): integrate biometrics\n\nFull details here",
                        "author": {
                            "name": "Claude",
                            "date": "2026-08-31T13:45:00Z"
                        }
                    }
                }
            ]
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                when {
                    url.contains("/user/repos") -> Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(mockReposJson.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                    url.contains("/commits") -> Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(mockCommitsJson.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                    else -> Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("[]".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
            })
            .build()

        val connector = GitHubConnector(mockClient)
        val syncer = GitHubSyncer(database, keystoreManager, connector)

        // 4. Run sync
        val syncResult = syncer.sync(SyncMode.FOREGROUND)
        assertTrue(syncResult.isSuccess)

        // 5. Verify Parent Project Entity State: Planning preserved, GitHub telemetry updated
        val postSyncProject = database.projectDao().getProjectById("secondme")
        assertNotNull(postSyncProject)
        assertEquals("SecondMe", postSyncProject!!.name)
        assertEquals("Updated Remote GitHub Description", postSyncProject.description)
        assertEquals("develop", postSyncProject.repositoryDefaultBranch)
        assertEquals("Maazkhan88/SecondMe", postSyncProject.repositoryFullName)

        // Verify DMS planning state is strictly preserved
        assertEquals(ProjectStatus.ON_TRACK, postSyncProject.status)
        assertEquals("2026-09-15", postSyncProject.dueDate)
        assertEquals("Private Beta Launch", postSyncProject.nextMilestone)
        assertEquals(0.63f, postSyncProject.manualProgressOverride!!, 0.001f)
        assertEquals(0.20f, postSyncProject.planningWeight, 0.001f)
        assertEquals(0.40f, postSyncProject.developmentWeight, 0.001f)
        assertTrue(postSyncProject.isMvp)

        // 6. Verify Child Data Preservation (CASCADE triggers did NOT delete rows)
        val details = database.projectDao().getProjectWithDetailsFlow("secondme").first()
        assertNotNull(details)

        // Milestones
        assertEquals(1, details!!.milestones.size)
        assertEquals("Private Beta", details.milestones.first().title)
        assertEquals("m_beta", details.milestones.first().id)

        // Blockers
        assertEquals(1, details.blockers.size)
        assertEquals("Awaiting Apple entitlement", details.blockers.first().description)
        assertEquals("b_apple", details.blockers.first().id)

        // Activities: Local activity survived AND new commit activity was appended
        assertEquals(2, details.activities.size)
        val localActFound = details.activities.any { it.id == "act_roadmap_change" && it.title == "User changed roadmap" }
        val commitActFound = details.activities.any { it.id.startsWith("gh_") && it.title.contains("feat(auth)") }
        assertTrue("Local user activity must survive sync", localActFound)
        assertTrue("GitHub commit activity must be added alongside local activity", commitActFound)

        // Tasks
        assertEquals(1, details.tasks.size)
        assertEquals("Prepare onboarding", details.tasks.first().title)
        assertEquals("task_onboarding", details.tasks.first().id)
    }
}
