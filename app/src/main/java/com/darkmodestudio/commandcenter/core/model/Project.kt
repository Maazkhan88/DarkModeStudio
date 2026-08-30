package com.darkmodestudio.commandcenter.core.model

import com.darkmodestudio.commandcenter.core.designsystem.component.MilestoneItem
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle

enum class ProjectStatus(val displayName: String, val nodeStyle: NodeStyle) {
    ON_TRACK("On Track", NodeStyle.SOLID),
    IN_PROGRESS("In Progress", NodeStyle.DOUBLE_RING),
    WAITING("Waiting", NodeStyle.HOLLOW),
    AT_RISK("At Risk", NodeStyle.DOTTED),
    BLOCKED("Blocked", NodeStyle.SLASH),
    DONE("Done", NodeStyle.CHECK)
}

data class PhaseDistribution(
    val planning: Float = 0.15f,
    val development: Float = 0.45f,
    val testing: Float = 0.20f,
    val deployment: Float = 0.20f
)

data class ProjectActivity(
    val id: String,
    val title: String,
    val author: String,
    val hash: String? = null,
    val timestamp: String
)

data class ProjectBlocker(
    val id: String,
    val description: String,
    val severity: String = "High",
    val duration: String
)

data class Project(
    val id: String,
    val name: String,
    val description: String,
    val iconTag: String, // e.g. "GC", "SM", "PT", "AG", "PN"
    val status: ProjectStatus,
    val progress: Float, // 0.0 to 1.0
    val owner: String,
    val createdAt: String,
    val dueDate: String,
    val nextMilestone: String,
    val isMvp: Boolean = false,
    val lastUpdate: String = "Just now",
    val phases: PhaseDistribution = PhaseDistribution(),
    val milestones: List<MilestoneItem> = emptyList(),
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
    val pendingTasks: Int = 0,
    val assignedAgents: List<String> = emptyList(),
    val blockers: List<ProjectBlocker> = emptyList(),
    val activities: List<ProjectActivity> = emptyList(),
    val repositoryFullName: String? = null
)
