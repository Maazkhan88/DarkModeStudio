package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.designsystem.component.MilestoneItem
import com.darkmodestudio.commandcenter.core.model.PhaseDistribution
import com.darkmodestudio.commandcenter.core.model.Project
import com.darkmodestudio.commandcenter.core.model.ProjectActivity
import com.darkmodestudio.commandcenter.core.model.ProjectBlocker
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProjectRepository {

    private val _projects = MutableStateFlow(
        listOf(
            Project(
                id = "secondme",
                name = "SecondMe",
                description = "Personal AI memory & continuous persona clone",
                iconTag = "SM",
                status = ProjectStatus.IN_PROGRESS,
                progress = 0.54f,
                owner = "Founder",
                createdAt = "Aug 01, 2026",
                dueDate = "Oct 01, 2026",
                nextMilestone = "User Memory Sync",
                isMvp = true,
                lastUpdate = "12m ago",
                phases = PhaseDistribution(
                    planning = 0.15f,
                    development = 0.45f,
                    testing = 0.20f,
                    deployment = 0.20f
                ),
                milestones = listOf(
                    MilestoneItem("Architecture", isCompleted = true, date = "Aug 10"),
                    MilestoneItem("Memory Engine", isCompleted = true, date = "Aug 22"),
                    MilestoneItem("Sync Layer", isActive = true, date = "Sep 05"),
                    MilestoneItem("Staging v1.0", isCompleted = false, date = "Sep 20")
                ),
                totalTasks = 12,
                doneTasks = 6,
                pendingTasks = 6,
                assignedAgents = listOf("Codex", "Claude", "Antigravity"),
                blockers = listOf(
                    ProjectBlocker(
                        id = "b1",
                        description = "Blocked on Cloudflare Workers AI rate-limit tier elevation",
                        severity = "High",
                        duration = "6 hours"
                    )
                ),
                activities = listOf(
                    ProjectActivity(
                        id = "a1",
                        title = "feat: add user memory sync",
                        author = "Codex",
                        hash = "a1b2c3d",
                        timestamp = "18m ago"
                    ),
                    ProjectActivity(
                        id = "a2",
                        title = "Deploy: staging v0.4.2",
                        author = "Claude",
                        hash = "deployed",
                        timestamp = "1h ago"
                    ),
                    ProjectActivity(
                        id = "a3",
                        title = "fix: resolve auth edge case",
                        author = "Antigravity",
                        hash = "d4e5f6a",
                        timestamp = "3h ago"
                    )
                )
            ),
            Project(
                id = "ghostcart",
                name = "GhostCart",
                description = "Ultra-fast headless commerce checkout system",
                iconTag = "GC",
                status = ProjectStatus.ON_TRACK,
                progress = 0.78f,
                owner = "Founder",
                createdAt = "Jul 15, 2026",
                dueDate = "Sep 15, 2026",
                nextMilestone = "Production Load Test",
                isMvp = false,
                lastUpdate = "4m ago",
                totalTasks = 14,
                doneTasks = 11,
                pendingTasks = 3,
                assignedAgents = listOf("Codex", "Claude"),
                milestones = listOf(
                    MilestoneItem("Core Engine", isCompleted = true),
                    MilestoneItem("Cart API", isCompleted = true),
                    MilestoneItem("Load Test", isActive = true),
                    MilestoneItem("Release", isCompleted = false)
                )
            ),
            Project(
                id = "proptree",
                name = "Proptree",
                description = "Real estate intelligence graph and deal radar",
                iconTag = "PT",
                status = ProjectStatus.WAITING,
                progress = 0.32f,
                owner = "Founder",
                createdAt = "Aug 10, 2026",
                dueDate = "Nov 10, 2026",
                nextMilestone = "Data Schema Ingestion",
                isMvp = false,
                lastUpdate = "2h ago",
                totalTasks = 12,
                doneTasks = 4,
                pendingTasks = 8,
                assignedAgents = listOf("Claude")
            ),
            Project(
                id = "agstudio",
                name = "AG Studio",
                description = "Autonomous coding agent IDE & swarm orchestrator",
                iconTag = "AG",
                status = ProjectStatus.ON_TRACK,
                progress = 0.91f,
                owner = "Founder",
                createdAt = "Jun 01, 2026",
                dueDate = "Sep 05, 2026",
                nextMilestone = "Release v2.0",
                isMvp = false,
                lastUpdate = "1m ago",
                totalTasks = 21,
                doneTasks = 19,
                pendingTasks = 2,
                assignedAgents = listOf("Antigravity", "Codex")
            ),
            Project(
                id = "pioneer",
                name = "Pioneer",
                description = "Low-latency streaming audio bridge & DSP node",
                iconTag = "PN",
                status = ProjectStatus.BLOCKED,
                progress = 0.15f,
                owner = "Founder",
                createdAt = "Aug 20, 2026",
                dueDate = "Dec 01, 2026",
                nextMilestone = "Core DSP Pipeline",
                isMvp = false,
                lastUpdate = "5h ago",
                totalTasks = 15,
                doneTasks = 2,
                pendingTasks = 13,
                assignedAgents = listOf("Codex"),
                blockers = listOf(
                    ProjectBlocker(
                        id = "b2",
                        description = "Native C++ bridge crash on Android 15 ARM64",
                        severity = "High",
                        duration = "2 days"
                    )
                )
            )
        )
    )

    val projects: Flow<List<Project>> = _projects.asStateFlow()

    fun getProject(id: String): Project? {
        return _projects.value.find { it.id == id } ?: _projects.value.firstOrNull()
    }
}
