package com.darkmodestudio.commandcenter.core.data.repository

import com.darkmodestudio.commandcenter.core.database.dao.ProjectDao
import com.darkmodestudio.commandcenter.core.database.dao.ProjectWithDetails
import com.darkmodestudio.commandcenter.core.database.entity.ProjectBlockerEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectEntity
import com.darkmodestudio.commandcenter.core.database.entity.ProjectMilestoneEntity
import com.darkmodestudio.commandcenter.core.designsystem.component.MilestoneItem
import com.darkmodestudio.commandcenter.core.model.PhaseDistribution
import com.darkmodestudio.commandcenter.core.model.Project
import com.darkmodestudio.commandcenter.core.model.ProjectActivity
import com.darkmodestudio.commandcenter.core.model.ProjectBlocker
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ProjectRepository(private val projectDao: ProjectDao? = null) {

    val projects: Flow<List<Project>> = projectDao?.getProjectsWithDetailsFlow()?.map { list ->
        list.map { it.toDomain() }
    } ?: flowOf(defaultProjects)

    fun getProjectFlow(id: String): Flow<Project?> {
        return projectDao?.getProjectWithDetailsFlow(id)?.map { it?.toDomain() }
            ?: flowOf(defaultProjects.find { it.id == id } ?: defaultProjects.first())
    }

    fun getProject(id: String): Project? {
        return defaultProjects.find { it.id == id } ?: defaultProjects.firstOrNull()
    }

    suspend fun createProject(
        name: String,
        description: String,
        iconTag: String,
        status: ProjectStatus,
        dueDate: String,
        nextMilestone: String,
        isMvp: Boolean = false,
        manualOverride: Float? = null
    ): String {
        val id = name.lowercase().replace(" ", "").replace("-", "") + "_" + System.currentTimeMillis().toString().takeLast(4)
        val entity = ProjectEntity(
            id = id,
            name = name,
            description = description,
            iconTag = if (iconTag.isNotBlank()) iconTag.uppercase().take(2) else name.take(2).uppercase(),
            status = status,
            isMvp = isMvp,
            owner = "Founder",
            createdAt = "Today",
            dueDate = dueDate,
            nextMilestone = nextMilestone,
            manualProgressOverride = manualOverride,
            lastUpdate = "Just now"
        )
        projectDao?.insertProject(entity)
        return id
    }

    suspend fun updateProject(project: Project) {
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            description = project.description,
            iconTag = project.iconTag,
            status = project.status,
            isMvp = project.isMvp,
            owner = project.owner,
            createdAt = project.createdAt,
            dueDate = project.dueDate,
            nextMilestone = project.nextMilestone,
            manualProgressOverride = project.progress,
            planningWeight = project.phases.planning,
            developmentWeight = project.phases.development,
            testingWeight = project.phases.testing,
            deploymentWeight = project.phases.deployment,
            lastUpdate = "Just now"
        )
        projectDao?.updateProject(entity)
    }

    suspend fun deleteProject(projectId: String) {
        projectDao?.deleteProject(projectId)
    }

    suspend fun addMilestone(projectId: String, title: String, date: String? = null) {
        val milestone = ProjectMilestoneEntity(
            id = "m_" + System.currentTimeMillis(),
            projectId = projectId,
            title = title,
            isCompleted = false,
            isActive = true,
            date = date
        )
        projectDao?.insertMilestones(listOf(milestone))
    }

    suspend fun addBlocker(projectId: String, description: String, severity: String = "High") {
        val blocker = ProjectBlockerEntity(
            id = "b_" + System.currentTimeMillis(),
            projectId = projectId,
            description = description,
            severity = severity,
            duration = "Just now"
        )
        projectDao?.insertBlockers(listOf(blocker))
    }

    companion object {
        val defaultProjects = listOf(
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
                phases = PhaseDistribution(0.15f, 0.45f, 0.20f, 0.20f),
                milestones = listOf(
                    MilestoneItem("Architecture", isCompleted = true, isActive = false, date = "Aug 10"),
                    MilestoneItem("Memory Engine", isCompleted = true, isActive = false, date = "Aug 22"),
                    MilestoneItem("Sync Layer", isCompleted = false, isActive = true, date = "Sep 05"),
                    MilestoneItem("Staging v1.0", isCompleted = false, isActive = false, date = "Sep 20")
                ),
                totalTasks = 12,
                doneTasks = 6,
                pendingTasks = 6,
                assignedAgents = listOf("Codex", "Claude", "Antigravity"),
                blockers = listOf(
                    ProjectBlocker("b1", "Blocked on Cloudflare Workers AI rate-limit tier elevation", "High", "6 hours")
                ),
                activities = listOf(
                    ProjectActivity("a1", "feat: add user memory sync", "Codex", "a1b2c3d", "18m ago"),
                    ProjectActivity("a2", "Deploy: staging v0.4.2", "Claude", "deployed", "1h ago"),
                    ProjectActivity("a3", "fix: resolve auth edge case", "Antigravity", "d4e5f6a", "3h ago")
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
                lastUpdate = "4m ago"
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
                lastUpdate = "2h ago"
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
                lastUpdate = "1m ago"
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
                lastUpdate = "5h ago"
            )
        )
    }
}

private fun ProjectWithDetails.toDomain(): Project {
    val totalTaskCount = tasks.size.coerceAtLeast(1)
    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val pendingCount = tasks.count { it.status != TaskStatus.DONE }

    val calculated = if (project.manualProgressOverride != null) {
        project.manualProgressOverride
    } else {
        val milestoneProgress = if (milestones.isNotEmpty()) {
            milestones.count { it.isCompleted }.toFloat() / milestones.size
        } else 0f
        val taskProgress = doneCount.toFloat() / totalTaskCount
        ((milestoneProgress * 0.5f) + (taskProgress * 0.5f)).coerceIn(0f, 1f)
    }

    return Project(
        id = project.id,
        name = project.name,
        description = project.description,
        iconTag = project.iconTag,
        status = project.status,
        progress = calculated,
        owner = project.owner,
        createdAt = project.createdAt,
        dueDate = project.dueDate,
        nextMilestone = project.nextMilestone,
        isMvp = project.isMvp,
        lastUpdate = project.lastUpdate,
        phases = PhaseDistribution(
            planning = project.planningWeight,
            development = project.developmentWeight,
            testing = project.testingWeight,
            deployment = project.deploymentWeight
        ),
        milestones = milestones.map {
            MilestoneItem(
                title = it.title,
                isCompleted = it.isCompleted,
                isActive = it.isActive,
                date = it.date
            )
        },
        totalTasks = if (tasks.isNotEmpty()) tasks.size else 12,
        doneTasks = if (tasks.isNotEmpty()) doneCount else 6,
        pendingTasks = if (tasks.isNotEmpty()) pendingCount else 6,
        assignedAgents = listOf("Codex", "Claude", "Antigravity"),
        blockers = blockers.map {
            ProjectBlocker(
                id = it.id,
                description = it.description,
                severity = it.severity,
                duration = it.duration
            )
        },
        activities = activities.map {
            ProjectActivity(
                id = it.id,
                title = it.title,
                author = it.author,
                hash = it.hash,
                timestamp = it.timestamp
            )
        }
    )
}
