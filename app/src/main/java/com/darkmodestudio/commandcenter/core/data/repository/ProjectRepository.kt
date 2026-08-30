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
    } ?: flowOf(emptyList())

    fun getProjectFlow(id: String): Flow<Project?> {
        return projectDao?.getProjectWithDetailsFlow(id)?.map { it?.toDomain() }
            ?: flowOf(null)
    }

    suspend fun getProject(id: String): Project? {
        return projectDao?.getProjectById(id)?.let { entity ->
            Project(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                iconTag = entity.iconTag,
                status = entity.status,
                progress = entity.manualProgressOverride ?: 0f,
                owner = entity.owner,
                createdAt = entity.createdAt,
                dueDate = entity.dueDate,
                nextMilestone = entity.nextMilestone,
                isMvp = entity.isMvp,
                lastUpdate = entity.lastUpdate,
                repositoryFullName = entity.repositoryFullName,
                repositoryDefaultBranch = entity.repositoryDefaultBranch
            )
        }
    }

    suspend fun createProject(
        name: String,
        description: String,
        iconTag: String,
        status: ProjectStatus,
        dueDate: String,
        nextMilestone: String,
        isMvp: Boolean = false,
        manualOverride: Float? = null,
        repositoryFullName: String? = null,
        repositoryDefaultBranch: String? = null
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
            lastUpdate = "Just now",
            repositoryFullName = repositoryFullName,
            repositoryDefaultBranch = repositoryDefaultBranch
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
            lastUpdate = "Just now",
            repositoryFullName = project.repositoryFullName,
            repositoryDefaultBranch = project.repositoryDefaultBranch
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
}

private fun ProjectWithDetails.toDomain(): Project {
    val doneCount = tasks.count { it.status == TaskStatus.DONE }
    val pendingCount = tasks.count { it.status != TaskStatus.DONE }

    val calculatedProgress = if (project.manualProgressOverride != null) {
        project.manualProgressOverride
    } else if (milestones.isEmpty() && tasks.isEmpty()) {
        0f
    } else {
        val milestoneProgress = if (milestones.isNotEmpty()) {
            milestones.count { it.isCompleted }.toFloat() / milestones.size
        } else 0f
        val taskProgress = if (tasks.isNotEmpty()) {
            doneCount.toFloat() / tasks.size
        } else 0f

        if (milestones.isNotEmpty() && tasks.isNotEmpty()) {
            ((milestoneProgress * 0.5f) + (taskProgress * 0.5f)).coerceIn(0f, 1f)
        } else if (milestones.isNotEmpty()) {
            milestoneProgress
        } else {
            taskProgress
        }
    }

    val assigned = tasks.mapNotNull { it.assignedAgent }.filter { it.isNotBlank() }.distinct()

    return Project(
        id = project.id,
        name = project.name,
        description = project.description,
        iconTag = project.iconTag,
        status = project.status,
        progress = calculatedProgress,
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
        totalTasks = tasks.size,
        doneTasks = doneCount,
        pendingTasks = pendingCount,
        assignedAgents = assigned,
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
        },
        repositoryFullName = project.repositoryFullName,
        repositoryDefaultBranch = project.repositoryDefaultBranch
    )
}
