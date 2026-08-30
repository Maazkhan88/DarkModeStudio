package com.darkmodestudio.commandcenter.core.model

enum class AgentProvider(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic"),
    ANTIGRAVITY("Antigravity"),
    CUSTOM("Custom")
}

data class Agent(
    val id: String,
    val name: String,
    val provider: AgentProvider,
    val mode: String = "Pro",
    val speed: String = "Fast",
    val runsUsed: Int,
    val runsTotal: Int,
    val messagesUsed: Int,
    val messagesTotal: Int,
    val tasksUsed: Int,
    val tasksTotal: Int,
    val currentTask: String,
    val statusText: String,
    val usagePercentage: Float, // 0.0 to 1.0
    val historyPoints: List<Float> = listOf(0.2f, 0.35f, 0.45f, 0.6f, 0.55f, 0.8f, 0.68f)
)
