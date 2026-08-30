package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.model.Agent
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AgentRepository {

    private val _agents = MutableStateFlow(
        listOf(
            Agent(
                id = "codex",
                name = "Codex",
                provider = AgentProvider.OPENAI,
                mode = "Pro",
                speed = "Fast",
                runsUsed = 225,
                runsTotal = 500,
                messagesUsed = 2150,
                messagesTotal = 5000,
                tasksUsed = 48,
                tasksTotal = 100,
                currentTask = "Refactor auth module and add unit tests",
                statusText = "In Progress • 68%",
                usagePercentage = 0.68f,
                historyPoints = listOf(0.2f, 0.35f, 0.45f, 0.6f, 0.55f, 0.72f, 0.68f)
            ),
            Agent(
                id = "claude",
                name = "Claude",
                provider = AgentProvider.ANTHROPIC,
                mode = "Opus",
                speed = "Pro",
                runsUsed = 160,
                runsTotal = 600,
                messagesUsed = 4320,
                messagesTotal = 10000,
                tasksUsed = 112,
                tasksTotal = 300,
                currentTask = "Synthesize GhostCart API schema and edge endpoints",
                statusText = "Active • 82%",
                usagePercentage = 0.82f,
                historyPoints = listOf(0.3f, 0.4f, 0.65f, 0.7f, 0.85f, 0.8f, 0.82f)
            ),
            Agent(
                id = "antigravity",
                name = "Antigravity",
                provider = AgentProvider.ANTIGRAVITY,
                mode = "Swarm",
                speed = "Max",
                runsUsed = 94,
                runsTotal = 400,
                messagesUsed = 2150,
                messagesTotal = 5000,
                tasksUsed = 53,
                tasksTotal = 200,
                currentTask = "Orchestrate multi-agent build pipeline & QA review",
                statusText = "Ready • 41%",
                usagePercentage = 0.41f,
                historyPoints = listOf(0.1f, 0.25f, 0.3f, 0.45f, 0.38f, 0.42f, 0.41f)
            ),
            Agent(
                id = "custom_agent",
                name = "Custom Agent",
                provider = AgentProvider.CUSTOM,
                mode = "Local",
                speed = "Manual",
                runsUsed = 0,
                runsTotal = 100,
                messagesUsed = 0,
                messagesTotal = 1000,
                tasksUsed = 0,
                tasksTotal = 50,
                currentTask = "Idle — Local manual sync adapter connected",
                statusText = "Standby • 0%",
                usagePercentage = 0.0f,
                historyPoints = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            )
        )
    )

    val agents: Flow<List<Agent>> = _agents.asStateFlow()

    val totalRunsUsed: Int = 479
    val totalRunsLimit: Int = 1500

    val totalMessagesUsed: Int = 8620
    val totalMessagesLimit: Int = 20000

    val totalTasksUsed: Int = 213
    val totalTasksLimit: Int = 600

    val globalHistory: List<Float> = listOf(0.2f, 0.32f, 0.48f, 0.58f, 0.62f, 0.74f, 0.69f)
}
