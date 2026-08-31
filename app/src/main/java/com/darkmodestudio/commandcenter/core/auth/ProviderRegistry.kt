package com.darkmodestudio.commandcenter.core.auth

enum class ProviderCategory(val displayName: String) {
    ALL("All"),
    AI_AGENTS("AI Agents"),
    AI_PROVIDERS("AI Providers"),
    SOURCE_CONTROL("Source Control"),
    CLOUD_HOSTING("Cloud & Hosting"),
    DATABASE_BACKEND("Database & Backend"),
    CUSTOM("Custom")
}

sealed interface AuthMethod {
    val displayName: String

    data object OAuthPkce : AuthMethod {
        override val displayName = "OAuth 2.0 (PKCE)"
    }
    data object OAuthBackend : AuthMethod {
        override val displayName = "OAuth 2.0 (Backend Broker)"
    }
    data object LocalRuntimeSession : AuthMethod {
        override val displayName = "Desktop Runtime Session"
    }
    data object ApiToken : AuthMethod {
        override val displayName = "Personal Access Token"
    }
    data object ApiKey : AuthMethod {
        override val displayName = "API Key"
    }
    data object ServiceAccount : AuthMethod {
        override val displayName = "Service Account Key"
    }
    data object CustomCredential : AuthMethod {
        override val displayName = "Custom Credential"
    }
}

enum class ProviderCapability(val label: String) {
    READ_TELEMETRY("Read Telemetry"),
    CODE_SYNC("Repository & Code Sync"),
    DEPLOYMENTS("Deployments & Hosting"),
    DATABASE_MGMT("Database Management"),
    AGENT_ORCHESTRATION("Autonomous Agent Orchestration"),
    CI_WORKFLOWS("CI/CD Actions")
}

enum class ConnectionState(val displayName: String) {
    DISCONNECTED("Disconnected"),
    SETUP_REQUIRED("Setup Required"),
    AUTHORIZING("Authorizing..."),
    CONNECTED("Connected"),
    EXPIRED("Session Expired"),
    DEGRADED("Degraded"),
    ERROR("Error"),
    RUNTIME_OFFLINE("Runtime Offline")
}

data class ProviderDefinition(
    val id: String,
    val displayName: String,
    val category: ProviderCategory,
    val iconTag: String,
    val authMethods: List<AuthMethod>,
    val capabilities: Set<ProviderCapability>,
    val connectorAvailable: Boolean = true,
    val runtimeRequired: Boolean = false,
    val description: String,
    val recommendedActionLabel: String,
    val externalSetupDoc: String? = null
)

object ProviderRegistry {

    private val providers = listOf(
        // 1. Source Control
        ProviderDefinition(
            id = "github",
            displayName = "GitHub",
            category = ProviderCategory.SOURCE_CONTROL,
            iconTag = "GH",
            authMethods = listOf(AuthMethod.OAuthPkce, AuthMethod.ApiToken),
            capabilities = setOf(ProviderCapability.CODE_SYNC, ProviderCapability.CI_WORKFLOWS, ProviderCapability.READ_TELEMETRY),
            description = "Repositories, live commit streaming, workflows, and PR telemetry.",
            recommendedActionLabel = "Sign in with GitHub",
            externalSetupDoc = "docs/connect-auth-provider-setup.md#github"
        ),

        // 2. Cloud & Hosting
        ProviderDefinition(
            id = "cloudflare",
            displayName = "Cloudflare",
            category = ProviderCategory.CLOUD_HOSTING,
            iconTag = "CF",
            authMethods = listOf(AuthMethod.OAuthPkce, AuthMethod.ApiToken),
            capabilities = setOf(ProviderCapability.DEPLOYMENTS, ProviderCapability.READ_TELEMETRY),
            description = "Edge networks, DNS zones, and Workers deployments.",
            recommendedActionLabel = "Sign in with Cloudflare",
            externalSetupDoc = "docs/connect-auth-provider-setup.md#cloudflare"
        ),
        ProviderDefinition(
            id = "vercel",
            displayName = "Vercel",
            category = ProviderCategory.CLOUD_HOSTING,
            iconTag = "VR",
            authMethods = listOf(AuthMethod.OAuthBackend, AuthMethod.ApiToken),
            capabilities = setOf(ProviderCapability.DEPLOYMENTS, ProviderCapability.READ_TELEMETRY),
            description = "Frontend hosting, instant preview deployments, and edge network metrics.",
            recommendedActionLabel = "Sign in with Vercel",
            externalSetupDoc = "docs/connect-auth-provider-setup.md#vercel"
        ),
        ProviderDefinition(
            id = "firebase",
            displayName = "Firebase / Google Cloud",
            category = ProviderCategory.CLOUD_HOSTING,
            iconTag = "FB",
            authMethods = listOf(AuthMethod.OAuthPkce, AuthMethod.ServiceAccount),
            capabilities = setOf(ProviderCapability.DEPLOYMENTS, ProviderCapability.READ_TELEMETRY),
            description = "Google Cloud projects, Firebase mobile telemetry, and Crashlytics.",
            recommendedActionLabel = "Sign in with Google",
            externalSetupDoc = "docs/connect-auth-provider-setup.md#firebase--google-cloud"
        ),

        // 3. Database & Backend
        ProviderDefinition(
            id = "supabase",
            displayName = "Supabase",
            category = ProviderCategory.DATABASE_BACKEND,
            iconTag = "SB",
            authMethods = listOf(AuthMethod.OAuthPkce, AuthMethod.ApiToken),
            capabilities = setOf(ProviderCapability.DATABASE_MGMT, ProviderCapability.READ_TELEMETRY),
            description = "Postgres databases, REST APIs, and authentication services.",
            recommendedActionLabel = "Sign in with Supabase",
            externalSetupDoc = "docs/connect-auth-provider-setup.md#supabase"
        ),

        // 4. AI Providers
        ProviderDefinition(
            id = "openai_api",
            displayName = "OpenAI API",
            category = ProviderCategory.AI_PROVIDERS,
            iconTag = "OA",
            authMethods = listOf(AuthMethod.ApiKey),
            capabilities = setOf(ProviderCapability.AGENT_ORCHESTRATION),
            description = "Direct OpenAI API key for completions, embeddings, and fine-tuning.",
            recommendedActionLabel = "Configure API Key"
        ),
        ProviderDefinition(
            id = "anthropic_api",
            displayName = "Anthropic API",
            category = ProviderCategory.AI_PROVIDERS,
            iconTag = "AN",
            authMethods = listOf(AuthMethod.ApiKey),
            capabilities = setOf(ProviderCapability.AGENT_ORCHESTRATION),
            description = "Direct Anthropic API key for Claude 3.5 Sonnet and Opus API access.",
            recommendedActionLabel = "Configure API Key"
        ),

        // 5. AI Agents
        ProviderDefinition(
            id = "codex",
            displayName = "Codex",
            category = ProviderCategory.AI_AGENTS,
            iconTag = "CX",
            authMethods = listOf(AuthMethod.LocalRuntimeSession, AuthMethod.ApiKey),
            capabilities = setOf(ProviderCapability.AGENT_ORCHESTRATION, ProviderCapability.CODE_SYNC),
            runtimeRequired = true,
            description = "Lead Architect & Developer agent running through local OpenAI Codex CLI.",
            recommendedActionLabel = "Sign in with ChatGPT (via Desktop Host)"
        ),
        ProviderDefinition(
            id = "claude_code",
            displayName = "Claude Code",
            category = ProviderCategory.AI_AGENTS,
            iconTag = "CC",
            authMethods = listOf(AuthMethod.LocalRuntimeSession, AuthMethod.ApiKey),
            capabilities = setOf(ProviderCapability.AGENT_ORCHESTRATION, ProviderCapability.CODE_SYNC),
            runtimeRequired = true,
            description = "Anthropic's terminal agent connecting via official Claude subscription session.",
            recommendedActionLabel = "Sign in with Claude (via Desktop Host)"
        ),
        ProviderDefinition(
            id = "antigravity",
            displayName = "Antigravity",
            category = ProviderCategory.AI_AGENTS,
            iconTag = "AG",
            authMethods = listOf(AuthMethod.LocalRuntimeSession, AuthMethod.ApiKey),
            capabilities = setOf(ProviderCapability.AGENT_ORCHESTRATION, ProviderCapability.CODE_SYNC),
            runtimeRequired = true,
            description = "Google DeepMind advanced agentic assistant running via agy CLI and secure keyring.",
            recommendedActionLabel = "Connect Antigravity (via Desktop Host)"
        ),

        // 6. Custom
        ProviderDefinition(
            id = "custom",
            displayName = "Custom Service",
            category = ProviderCategory.CUSTOM,
            iconTag = "CS",
            authMethods = listOf(AuthMethod.CustomCredential),
            capabilities = setOf(ProviderCapability.READ_TELEMETRY),
            description = "Connect proprietary webhooks, internal build runners, or custom APIs.",
            recommendedActionLabel = "Add Custom Credential"
        )
    )

    fun getProviders(): List<ProviderDefinition> = providers

    fun getProvider(id: String): ProviderDefinition? = providers.find { it.id.equals(id, ignoreCase = true) }

    fun getProvidersByCategory(category: ProviderCategory): List<ProviderDefinition> {
        if (category == ProviderCategory.ALL) return providers
        return providers.filter { it.category == category }
    }

    fun searchProviders(query: String): List<ProviderDefinition> {
        if (query.isBlank()) return providers
        val q = query.trim().lowercase()
        return providers.filter {
            it.displayName.lowercase().contains(q) ||
            it.id.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.displayName.lowercase().contains(q) ||
            it.recommendedActionLabel.lowercase().contains(q)
        }
    }
}
