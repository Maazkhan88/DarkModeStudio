package com.darkmodestudio.commandcenter.core.data.repository

import androidx.room.Room
import com.darkmodestudio.commandcenter.core.database.DmsDatabase
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import com.darkmodestudio.commandcenter.core.database.entity.AppSettingsEntity
import com.darkmodestudio.commandcenter.core.database.entity.IntegrationEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.TaskEntity
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class RepositoriesTest {

    private lateinit var database: DmsDatabase
    private lateinit var projectRepository: ProjectRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var agentRepository: AgentRepository
    private lateinit var healthRepository: HealthRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, DmsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        projectRepository = ProjectRepository(database.projectDao())
        taskRepository = TaskRepository(database.taskDao())
        agentRepository = AgentRepository(database.agentDao())
        healthRepository = HealthRepository(database.integrationDao())
        notificationRepository = NotificationRepository(
            database.notificationDao(),
            database.reminderDao(),
            database.settingsDao()
        )
        settingsRepository = SettingsRepository(
            database.settingsDao(),
            database.automationDao()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun project_zeroTasks_displaysZeroCountsAndZeroProgress() = runBlocking {
        val projectEntity = ProjectEntity(
            id = "test_project",
            name = "Test Zero Project",
            description = "A project with 0 tasks",
            iconTag = "TZ",
            status = ProjectStatus.ON_TRACK,
            isMvp = false,
            owner = "Tester",
            createdAt = "2026-08-30",
            dueDate = "",
            nextMilestone = "None",
            manualProgressOverride = null
        )
        database.projectDao().insertProject(projectEntity)

        val projects = projectRepository.projects.first()
        assertEquals(1, projects.size)
        val project = projects.first()

        assertEquals("test_project", project.id)
        assertEquals(0, project.totalTasks)
        assertEquals(0, project.doneTasks)
        assertEquals(0, project.pendingTasks)
        assertEquals(0.0f, project.progress, 0.001f)
        assertTrue("Assigned agents must be empty when no tasks exist", project.assignedAgents.isEmpty())
    }

    @Test
    fun project_withTasks_derivesAssignedAgentsAndTaskCounts() = runBlocking {
        val projectEntity = ProjectEntity(
            id = "p1",
            name = "Project One",
            description = "Test Project",
            iconTag = "P1",
            status = ProjectStatus.IN_PROGRESS,
            isMvp = true,
            owner = "Maaz",
            createdAt = "2026-08-30",
            dueDate = "2026-09-30",
            nextMilestone = "Alpha",
            manualProgressOverride = null
        )
        database.projectDao().insertProject(projectEntity)

        val task1 = TaskEntity(
            id = "t1",
            projectId = "p1",
            projectName = "Project One",
            title = "Build Auth",
            status = TaskStatus.DONE,
            priority = TaskPriority.HIGH,
            assignedAgent = "Codex",
            dueTime = "12:00 PM"
        )
        val task2 = TaskEntity(
            id = "t2",
            projectId = "p1",
            projectName = "Project One",
            title = "Build UI",
            status = TaskStatus.PENDING,
            priority = TaskPriority.MEDIUM,
            assignedAgent = "Claude",
            dueTime = "04:00 PM"
        )
        database.taskDao().insertTasks(listOf(task1, task2))

        val projects = projectRepository.projects.first()
        val project = projects.first { it.id == "p1" }

        assertEquals(2, project.totalTasks)
        assertEquals(1, project.doneTasks)
        assertEquals(1, project.pendingTasks)
        assertEquals(0.5f, project.progress, 0.001f)
        assertEquals(listOf("Codex", "Claude"), project.assignedAgents)
    }

    @Test
    fun task_toggleStatus_updatesPersistenceInRoom() = runBlocking {
        taskRepository.createTask(
            title = "Test Toggle Task",
            description = "Testing status toggle",
            projectId = "p1",
            projectName = "Project One",
            priority = TaskPriority.HIGH,
            assignedAgent = "Antigravity",
            dueTime = "Today 5pm"
        )

        val initialTasks = taskRepository.tasks.first()
        assertEquals(1, initialTasks.size)
        val taskId = initialTasks.first().id
        assertEquals(TaskStatus.PENDING, initialTasks.first().status)

        taskRepository.toggleTask(taskId)

        val updatedTasks = taskRepository.tasks.first()
        assertEquals(TaskStatus.DONE, updatedTasks.first().status)
        assertNotNull(updatedTasks.first().completedAt)
    }

    @Test
    fun agent_usageSummary_computesRealDynamicMetrics() = runBlocking {
        val agent1 = AgentEntity(
            id = "agent_codex",
            name = "Codex",
            provider = AgentProvider.OPENAI,
            runsUsed = 50,
            runsTotal = 500,
            messagesUsed = 300,
            messagesTotal = 5000,
            tasksUsed = 10,
            tasksTotal = 100,
            currentTask = "Working",
            statusText = "Active",
            usagePercentage = 0.1f
        )
        database.agentDao().insertAgent(agent1)

        val snapshot = AgentUsageSnapshotEntity(
            agentId = "agent_codex",
            dataSource = "LOCAL_TELEMETRY",
            requestsUsed = 50,
            requestsLimit = 500,
            messagesUsed = 300,
            messagesLimit = 5000,
            tokensUsed = 15000
        )
        database.agentDao().insertUsageSnapshot(snapshot)

        val summary = agentRepository.usageSummary.first()
        assertTrue(summary.hasLocalData)
        assertEquals(50, summary.runsUsed)
        assertEquals(500, summary.runsTotal)
        assertEquals(300, summary.messagesUsed)
        assertEquals(5000, summary.messagesTotal)
        assertEquals(10, summary.tasksUsed)
        assertEquals(100, summary.tasksTotal)
        assertEquals(1, summary.historyPoints.size)
        assertEquals(0.1f, summary.historyPoints.first(), 0.001f)
    }

    @Test
    fun integration_andSettings_persistence() = runBlocking {
        val integration = IntegrationEntity(
            id = "github",
            name = "GitHub",
            category = "Code & CI/CD",
            isConnected = true,
            health = IntegrationHealth.OPERATIONAL,
            lastSync = "Now",
            primaryMetric = "All CI Passing"
        )
        database.integrationDao().insertIntegration(integration)

        val integrations = healthRepository.integrations.first()
        assertEquals(1, integrations.size)
        assertEquals("github", integrations.first().id)
        assertTrue(integrations.first().isConnected)

        val initialSettings = AppSettingsEntity(
            id = 1,
            biometricLock = true,
            dailyBriefing = false
        )
        database.settingsDao().insertOrUpdate(initialSettings)

        val bioLock = settingsRepository.biometricLock.first()
        val briefing = settingsRepository.dailyBriefing.first()
        assertTrue(bioLock)
        assertFalse(briefing)
    }
}
