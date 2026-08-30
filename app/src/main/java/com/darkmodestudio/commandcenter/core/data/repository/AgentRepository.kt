package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.AgentDao
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import com.darkmodestudio.commandcenter.core.model.Agent
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AgentRepository(private val agentDao: AgentDao? = null) {

    val agents: Flow<List<Agent>> = agentDao?.getAgentsFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultAgents)

    val totalRunsUsed: Int = 479
    val totalRunsLimit: Int = 1500

    val totalMessagesUsed: Int = 8620
    val totalMessagesLimit: Int = 20000

    val totalTasksUsed: Int = 213
    val totalTasksLimit: Int = 600

    val globalHistory: List<Float> = listOf(0.2f, 0.32f, 0.48f, 0.58f, 0.62f, 0.74f, 0.69f)

    suspend fun addAgent(
        name: String,
        provider: AgentProvider,
        mode: String = "Pro",
        speed: String = "Fast",
        runsLimit: Int = 500,
        messagesLimit: Int = 5000,
        tasksLimit: Int = 100
    ): String {
        val id = "agent_" + System.currentTimeMillis()
        val entity = AgentEntity(
            id = id,
            name = name,
            provider = provider,
            mode = mode,
            speed = speed,
            runsUsed = 0,
            runsTotal = runsLimit,
            messagesUsed = 0,
            messagesTotal = messagesLimit,
            tasksUsed = 0,
            tasksTotal = tasksLimit,
            currentTask = "Ready for execution",
            statusText = "Standby • 0%",
            usagePercentage = 0.0f
        )
        agentDao?.insertAgent(entity)
        return id
    }

    suspend fun recordUsageSnapshot(
        agentId: String,
        dataSource: String,
        requestsUsed: Int,
        requestsLimit: Int,
        messagesUsed: Int,
        messagesLimit: Int,
        tokensUsed: Long
    ) {
        val snapshot = AgentUsageSnapshotEntity(
            agentId = agentId,
            dataSource = dataSource,
            requestsUsed = requestsUsed,
            requestsLimit = requestsLimit,
            messagesUsed = messagesUsed,
            messagesLimit = messagesLimit,
            tokensUsed = tokensUsed
        )
        agentDao?.insertUsageSnapshot(snapshot)
    }

    companion object {
        val defaultAgents = listOf(
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
    }
}

private fun AgentEntity.toDomain(): Agent {
    return Agent(
        id = id,
        name = name,
        provider = provider,
        mode = mode,
        speed = speed,
        runsUsed = runsUsed,
        runsTotal = runsTotal,
        messagesUsed = messagesUsed,
        messagesTotal = messagesTotal,
        tasksUsed = tasksUsed,
        tasksTotal = tasksTotal,
        currentTask = currentTask,
        statusText = statusText,
        usagePercentage = usagePercentage,
        historyPoints = when (id) {
            "codex" -> listOf(0.2f, 0.35f, 0.45f, 0.6f, 0.55f, 0.72f, 0.68f)
            "claude" -> listOf(0.3f, 0.4f, 0.65f, 0.7f, 0.85f, 0.8f, 0.82f)
            "antigravity" -> listOf(0.1f, 0.25f, 0.3f, 0.45f, 0.38f, 0.42f, 0.41f)
            else -> listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        }
    )
}
