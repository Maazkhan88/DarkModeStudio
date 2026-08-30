package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.data.adapter.AnthropicAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.AntigravityAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.ManualAgentAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.OpenAIAdapter
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoriesTest {

    @Test
    fun testProjectRepositoryProvidesRequiredProjects() = runBlocking {
        val repo = ProjectRepository()
        val projects = repo.projects.first()

        assertEquals(5, projects.size)
        val secondMe = projects.find { it.id == "secondme" }
        assertNotNull(secondMe)
        assertEquals("SecondMe", secondMe?.name)
        assertTrue(secondMe?.isMvp == true)
        assertEquals(0.54f, secondMe?.progress ?: 0f, 0.01f)
    }

    @Test
    fun testTaskRepositoryToggleAndSearch() = runBlocking {
        val repo = TaskRepository()
        val tasks = repo.tasks.first()
        assertTrue(tasks.isNotEmpty())

        val searchResults = repo.searchTasksFlow("Auth").first()
        assertTrue(searchResults.any { it.title.contains("Auth", ignoreCase = true) })
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
    }

    @Test
    fun testHealthRepositorySummary() = runBlocking {
        val repo = HealthRepository()
        val integrations = repo.integrations.first()
        val summary = repo.summaryFlow.first()

        assertEquals(7, integrations.size)
        assertEquals(7, summary.connectedCount)
        assertEquals(1, summary.degradedCount)
        assertEquals(0, summary.disconnectedCount)
        assertTrue(summary.healthScore > 0.8f)
    }

    @Test
    fun testNotificationRepositoryReminders() = runBlocking {
        val repo = NotificationRepository()
        val notifications = repo.notifications.first()
        val reminders = repo.reminders.first()

        assertEquals(5, notifications.size)
        assertEquals(3, reminders.size)
        assertTrue(reminders.all { it.isEnabled })
    }

    @Test
    fun testSettingsRepositoryToggles() = runBlocking {
        val repo = SettingsRepository()
        val profile = repo.userProfile.first()
        val stats = repo.automationStats.first()
        val biometric = repo.biometricLock.first()

        assertEquals("AG", profile.initials)
        assertEquals("Antigravity Founder", profile.name)
        assertEquals(4, stats.activeRules)
        assertEquals(2, stats.inactiveRules)
        assertEquals(128, stats.executionsLast24h)
        assertTrue(biometric)
    }

    @Test
    fun testAgentProviderAdapters() = runBlocking {
        val openai = OpenAIAdapter()
        val anthropic = AnthropicAdapter()
        val antigravity = AntigravityAdapter()
        val manual = ManualAgentAdapter()

        assertEquals(AgentProvider.OPENAI, openai.provider)
        assertEquals(AgentProvider.ANTHROPIC, anthropic.provider)
        assertEquals(AgentProvider.ANTIGRAVITY, antigravity.provider)
        assertEquals(AgentProvider.CUSTOM, manual.provider)

        assertTrue(openai.syncQuota())
        assertTrue(anthropic.syncQuota())
        assertTrue(antigravity.syncQuota())
        assertFalse(manual.syncQuota())
    }
}
