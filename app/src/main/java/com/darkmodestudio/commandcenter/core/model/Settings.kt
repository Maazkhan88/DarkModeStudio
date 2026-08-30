package com.darkmodestudio.commandcenter.core.model

data class UserProfile(
    val initials: String = "AG",
    val name: String = "Antigravity Founder",
    val email: String = "operator@darkmodestudio.internal",
    val plan: String = "Enterprise Command Tier",
    val projectCount: Int = 5,
    val automationsCount: Int = 6,
    val syncedDataSize: String = "14.2 MB",
    val uptime: String = "99.98%"
)

data class AutomationStats(
    val activeRules: Int = 4,
    val inactiveRules: Int = 2,
    val executionsLast24h: Int = 128
)

data class NotificationToggleState(
    val pushReminders: Boolean = true,
    val buildAlerts: Boolean = true,
    val taskDeadlines: Boolean = true,
    val agentLimitWarnings: Boolean = true,
    val platformIncidents: Boolean = true,
    val dailyBriefing: Boolean = true
)
