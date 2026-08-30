package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.data.adapter.AntigravityAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.AnthropicAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.ManualAgentAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.OpenAIAdapter
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoriesTest {

    @Test
    fun testProjectRepositoryProvidesRequiredProjects() = runBlocking {
        val repo = ProjectRepository()
        val list = repo.projects.first()

        assertTrue(list.isNotEmpty())
        assertNotNull(list.find { it.id == "secondme" })
        assertNotNull(list.find { it.id == "ghostcart" })
        assertNotNull(list.find { it.id == "proptree" })
        assertNotNull(list.find { it.id == "agstudio" })
        assertNotNull(list.find { it.id == "pioneer" })

        val secondMe = repo.getProject("secondme")
        assertNotNull(secondMe)
        assertEquals(ProjectStatus.IN_PROGRESS, secondMe?.status)
        assertTrue(secondMe?.isMvp == true)
        assertEquals(0.54f, secondMe?.progress ?: 0f, 0.01f)
    }

    @Test
    fun testTaskRepositoryToggle() = runBlocking {
        val repo = TaskRepository()
        val initialTasks = repo.tasks.first()
        val firstTask = initialTasks.first()

        assertEquals(TaskStatus.PENDING, firstTask.status)
        repo.toggleTask(firstTask.id)

        val updatedTasks = repo.tasks.first()
        val updatedFirstTask = updatedTasks.first { it.id == firstTask.id }
        assertEquals(TaskStatus.DONE, updatedFirstTask.status)

        repo.toggleTask(firstTask.id)
        val revertedFirstTask = repo.tasks.first().first { it.id == firstTask.id }
        assertEquals(TaskStatus.PENDING, revertedFirstTask.status)
    }

    @Test
    fun testAgentRepositoryMetrics() = runBlocking {
        val repo = AgentRepository()
        val agents = repo.agents.first()

        assertEquals(4, agents.size)
        assertEquals(479, repo.totalRunsUsed)
        assertEquals(1500, repo.totalRunsLimit)
        assertEquals(8620, repo.totalMessagesUsed)
        assertEquals(20000, repo.totalMessagesLimit)
        assertEquals(213, repo.totalTasksUsed)
        assertEquals(600, repo.totalTasksLimit)

        val codex = agents.find { it.id == "codex" }
        assertNotNull(codex)
        assertEquals(0.68f, codex?.usagePercentage ?: 0f, 0.01f)

        val claude = agents.find { it.id == "claude" }
        assertNotNull(claude)
        assertEquals(0.82f, claude?.usagePercentage ?: 0f, 0.01f)

        val ag = agents.find { it.id == "antigravity" }
        assertNotNull(ag)
        assertEquals(0.41f, ag?.usagePercentage ?: 0f, 0.01f)
    }

    @Test
    fun testHealthRepositorySummary() = runBlocking {
        val repo = HealthRepository()
        val integrations = repo.integrations.first()

        assertEquals(7, integrations.size)
        assertEquals(7, repo.summary.connectedCount)
        assertEquals(1, repo.summary.degradedCount)
        assertEquals(0, repo.summary.disconnectedCount)
        assertEquals(2, repo.summary.alertsCount)
        assertEquals(0.96f, repo.summary.healthScore, 0.01f)
    }

    @Test
    fun testNotificationRepositoryReminders() = runBlocking {
        val repo = NotificationRepository()
        val initialReminders = repo.reminders.first()
        val firstReminder = initialReminders.first()

        assertTrue(firstReminder.isEnabled)
        repo.toggleReminder(firstReminder.id)

        val updated = repo.reminders.first().first { it.id == firstReminder.id }
        assertTrue(!updated.isEnabled)
    }

    @Test
    fun testSettingsRepositoryToggles() = runBlocking {
        val repo = SettingsRepository()
        assertTrue(repo.biometricLock.first())
        repo.toggleBiometricLock()
        assertTrue(!repo.biometricLock.first())

        assertTrue(repo.dailyBriefing.first())
        repo.toggleDailyBriefing()
        assertTrue(!repo.dailyBriefing.first())
    }

    @Test
    fun testAgentProviderAdapters() {
        val openAi = OpenAIAdapter()
        assertEquals(AgentProvider.OPENAI, openAi.provider)
        assertTrue(openAi.syncQuota())

        val anthropic = AnthropicAdapter()
        assertEquals(AgentProvider.ANTHROPIC, anthropic.provider)
        assertTrue(anthropic.syncQuota())

        val antigravity = AntigravityAdapter()
        assertEquals(AgentProvider.ANTIGRAVITY, antigravity.provider)
        assertTrue(antigravity.syncQuota())

        val manual = ManualAgentAdapter()
        assertEquals(AgentProvider.CUSTOM, manual.provider)
        assertTrue(!manual.syncQuota())
    }
}
