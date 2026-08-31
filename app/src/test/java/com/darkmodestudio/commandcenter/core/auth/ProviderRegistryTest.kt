package com.darkmodestudio.commandcenter.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRegistryTest {

    @Test
    fun providerRegistry_containsAllRequiredServicesAndAgents() {
        val providers = ProviderRegistry.getProviders()

        val expectedIds = listOf(
            "github",
            "cloudflare",
            "vercel",
            "firebase",
            "supabase",
            "openai_api",
            "anthropic_api",
            "codex",
            "claude_code",
            "antigravity",
            "custom"
        )

        for (id in expectedIds) {
            val provider = ProviderRegistry.getProvider(id)
            assertNotNull("Provider $id must be registered in ProviderRegistry", provider)
            assertEquals(id, provider!!.id)
        }
    }

    @Test
    fun providerRegistry_hasNoDuplicateIds() {
        val providers = ProviderRegistry.getProviders()
        val ids = providers.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("Provider IDs in registry must be unique", ids.size, uniqueIds.size)
    }

    @Test
    fun providerRegistry_agentsRequireDesktopHostRuntime() {
        val codex = ProviderRegistry.getProvider("codex")
        val claude = ProviderRegistry.getProvider("claude_code")
        val antigravity = ProviderRegistry.getProvider("antigravity")

        assertNotNull(codex)
        assertNotNull(claude)
        assertNotNull(antigravity)

        assertTrue(codex!!.runtimeRequired)
        assertTrue(claude!!.runtimeRequired)
        assertTrue(antigravity!!.runtimeRequired)

        assertEquals(ProviderCategory.AI_AGENTS, codex.category)
        assertEquals(ProviderCategory.AI_AGENTS, claude.category)
        assertEquals(ProviderCategory.AI_AGENTS, antigravity.category)
    }

    @Test
    fun providerRegistry_categoryFilteringAndSearch_worksCorrectly() {
        val agents = ProviderRegistry.getProvidersByCategory(ProviderCategory.AI_AGENTS)
        assertEquals(3, agents.size)
        assertTrue(agents.all { it.category == ProviderCategory.AI_AGENTS })

        val sourceControl = ProviderRegistry.getProvidersByCategory(ProviderCategory.SOURCE_CONTROL)
        assertEquals(1, sourceControl.size)
        assertEquals("github", sourceControl.first().id)

        val searchResult = ProviderRegistry.searchProviders("postgres")
        assertTrue(searchResult.any { it.id == "supabase" })

        val agentSearch = ProviderRegistry.searchProviders("chatgpt")
        assertTrue(agentSearch.any { it.id == "codex" })
    }
}
