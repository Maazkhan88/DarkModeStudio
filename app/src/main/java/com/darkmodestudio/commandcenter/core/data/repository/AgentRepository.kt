package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.AgentDao
import com.darkmodestudio.commandcenter.core.database.entity.AgentEntity
import com.darkmodestudio.commandcenter.core.database.entity.AgentUsageSnapshotEntity
import com.darkmodestudio.commandcenter.core.model.Agent
import com.darkmodestudio.commandcenter.core.model.AgentProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class AgentUsageSummary(
    val runsUsed: Int = 0,
    val runsTotal: Int = 0,
    val messagesUsed: Int = 0,
    val messagesTotal: Int = 0,
    val tasksUsed: Int = 0,
    val tasksTotal: Int = 0,
    val historyPoints: List<Float> = emptyList(),
    val hasLocalData: Boolean = false
)

class AgentRepository(private val agentDao: AgentDao? = null) {

    val agents: Flow<List<Agent>> = agentDao?.getAgentsFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(emptyList())

    val usageSummary: Flow<AgentUsageSummary> = if (agentDao != null) {
        combine(
            agentDao.getAgentsFlow(),
            agentDao.getAllUsageSnapshotsFlow()
        ) { agentList, snapshotList ->
            val runsUsed = agentList.sumOf { it.runsUsed }
            val runsTotal = agentList.sumOf { it.runsTotal }.coerceAtLeast(1)
            val messagesUsed = agentList.sumOf { it.messagesUsed }
            val messagesTotal = agentList.sumOf { it.messagesTotal }.coerceAtLeast(1)
            val tasksUsed = agentList.sumOf { it.tasksUsed }
            val tasksTotal = agentList.sumOf { it.tasksTotal }.coerceAtLeast(1)

            val history = if (snapshotList.isNotEmpty()) {
                snapshotList.take(7).reversed().map {
                    (it.requestsUsed.toFloat() / it.requestsLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
                }
            } else {
                emptyList()
            }

            val hasData = runsUsed > 0 || messagesUsed > 0 || tasksUsed > 0 || snapshotList.isNotEmpty()

            AgentUsageSummary(
                runsUsed = runsUsed,
                runsTotal = runsTotal,
                messagesUsed = messagesUsed,
                messagesTotal = messagesTotal,
                tasksUsed = tasksUsed,
                tasksTotal = tasksTotal,
                historyPoints = history,
                hasLocalData = hasData
            )
        }
    } else {
        flowOf(AgentUsageSummary())
    }

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
        historyPoints = emptyList()
    )
}
