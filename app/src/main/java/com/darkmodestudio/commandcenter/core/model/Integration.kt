package com.darkmodestudio.commandcenter.core.model

enum class IntegrationHealth(val displayName: String) {
    OPERATIONAL("Operational"),
    DEGRADED("Degraded"),
    DISCONNECTED("Disconnected"),
    ALERT("Alert")
}

data class IntegrationMetric(
    val label: String,
    val value: String
)

data class IntegrationItem(
    val id: String,
    val name: String,
    val category: String,
    val isConnected: Boolean = true,
    val health: IntegrationHealth = IntegrationHealth.OPERATIONAL,
    val lastSync: String = "2m ago",
    val primaryMetric: String,
    val metrics: List<IntegrationMetric> = emptyList(),
    val activeAlerts: List<String> = emptyList()
)

data class PlatformHealthSummary(
    val connectedCount: Int = 7,
    val degradedCount: Int = 1,
    val disconnectedCount: Int = 0,
    val alertsCount: Int = 2,
    val healthScore: Float = 0.96f
)
