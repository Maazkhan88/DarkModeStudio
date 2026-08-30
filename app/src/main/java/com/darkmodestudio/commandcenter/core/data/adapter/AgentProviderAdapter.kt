package com.darkmodestudio.commandcenter.core.data.adapter

import com.darkmodestudio.commandcenter.core.model.Agent
import com.darkmodestudio.commandcenter.core.model.AgentProvider

interface AgentAdapter {
    val provider: AgentProvider
    fun fetchUsage(agent: Agent): Agent
    fun syncQuota(): Boolean
}

class OpenAIAdapter : AgentAdapter {
    override val provider = AgentProvider.OPENAI
    override fun fetchUsage(agent: Agent): Agent = agent
    override fun syncQuota(): Boolean = true
}

class AnthropicAdapter : AgentAdapter {
    override val provider = AgentProvider.ANTHROPIC
    override fun fetchUsage(agent: Agent): Agent = agent
    override fun syncQuota(): Boolean = true
}

class AntigravityAdapter : AgentAdapter {
    override val provider = AgentProvider.ANTIGRAVITY
    override fun fetchUsage(agent: Agent): Agent = agent
    override fun syncQuota(): Boolean = true
}

class ManualAgentAdapter : AgentAdapter {
    override val provider = AgentProvider.CUSTOM
    override fun fetchUsage(agent: Agent): Agent = agent
    override fun syncQuota(): Boolean = false
}
