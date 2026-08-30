package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.data.adapter.AnthropicAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.AntigravityAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.ManualAgentAdapter
import com.darkmodestudio.commandcenter.core.data.adapter.OpenAIAdapter
import com.darkmodestudio.commandcenter.core.model.AgentProvider
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

        assertTrue(projects.isNotEmpty())
        val secondMe = projects.find { it.id == "secondme" }
        assertNotNull(secondMe)
        assertEquals("SecondMe", secondMe?.name)
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
        assertTrue(repo.totalRunsUsed > 0)
        assertTrue(repo.totalRunsLimit > 0)
    }

    @Test
    fun testHealthRepositorySummary() = runBlocking {
        val repo = HealthRepository()
        val integrations = repo.integrations.first()
        val summary = repo.summaryFlow.first()

        assertTrue(integrations.isNotEmpty())
        assertTrue(summary.healthScore > 0f)
    }

    @Test
    fun testNotificationRepositoryReminders() = runBlocking {
        val repo = NotificationRepository()
        val notifications = repo.notifications.first()
        val reminders = repo.reminders.first()

        assertTrue(notifications.isNotEmpty())
        assertTrue(reminders.isNotEmpty())
    }

    @Test
    fun testSettingsRepositoryToggles() = runBlocking {
        val repo = SettingsRepository()
        val profile = repo.userProfile.first()
        val stats = repo.automationStats.first()
        val biometric = repo.biometricLock.first()

        assertEquals("AG", profile.initials)
        assertEquals("Antigravity Founder", profile.name)
        assertTrue(stats.activeRules > 0)
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
