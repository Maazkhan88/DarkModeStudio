package com.darkmodestudio.commandcenter.feature.projectdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.RepositoryFileEntry
import com.darkmodestudio.commandcenter.core.data.repository.RepositoryFilesRepository
import com.darkmodestudio.commandcenter.core.data.repository.RepositoryFilesState
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsHeroCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsMilestoneTimeline
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRail
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRing
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsVerticalTimeRail
import com.darkmodestudio.commandcenter.core.designsystem.component.TimeRailItem
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Task
import com.darkmodestudio.commandcenter.core.model.TaskPriority
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun ProjectDetailScreen(
    projectId: String,
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository? = null,
    repositoryFilesRepository: RepositoryFilesRepository? = null,
    onConnectGitHubClick: (() -> Unit)? = null,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val projectFlow by projectRepository.getProjectFlow(projectId).collectAsState(initial = null)
    val projectsList by projectRepository.projects.collectAsState(initial = emptyList())
    val project = projectFlow ?: projectsList.find { it.id == projectId } ?: projectsList.firstOrNull() ?: return

    val allTasks by (taskRepository?.tasks ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val projectTasks = allTasks.filter {
        it.projectId.equals(projectId, ignoreCase = true) || it.projectName.equals(project.name, ignoreCase = true)
    }

    val filesState by (repositoryFilesRepository?.filesState ?: flowOf(RepositoryFilesState.Disconnected)).collectAsState(initial = RepositoryFilesState.Disconnected)

    var selectedTab by remember { mutableStateOf("Overview") }

    LaunchedEffect(selectedTab, project.repositoryFullName, project.repositoryDefaultBranch) {
        if (selectedTab == "Files" && repositoryFilesRepository != null) {
            if (project.repositoryFullName.isNullOrBlank()) {
                repositoryFilesRepository.setNotLinked()
            } else {
                repositoryFilesRepository.loadDirectory(
                    repoFullName = project.repositoryFullName,
                    branch = project.repositoryDefaultBranch ?: "main"
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar with Back Arrow
        DmsTopBar(
            title = project.name,
            subtitle = "project detail",
            showBack = true,
            onBackClick = onBackClick,
            onNotificationClick = onNotificationClick,
            onAvatarClick = onAvatarClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO CARD: Progress Ring + Full Rail + Phase Distribution
            item {
                DmsHeroCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR22,
                    backgroundColor = DmsColors.Surface01,
                    padding = 18.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(DmsRadii.ShapeR12)
                                        .background(DmsColors.SurfaceSelected)
                                        .border(BorderStroke(1.dp, DmsColors.White20), DmsRadii.ShapeR12),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = project.iconTag,
                                        style = DmsTheme.typography.h3.copy(color = DmsColors.White)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = project.name,
                                            style = DmsTheme.typography.h2
                                        )
                                        if (project.isMvp) {
                                            DmsStatusCapsule(
                                                text = "MVP",
                                                height = 20.dp,
                                                borderColor = DmsColors.White48
                                            )
                                        }
                                    }
                                    Text(
                                        text = project.description,
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White64),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Progress Ring
                            DmsProgressRing(
                                progress = project.progress,
                                size = 72.dp,
                                strokeWidth = 7.dp,
                                centerContent = {
                                    Text(
                                        text = "${(project.progress * 100).toInt()}%",
                                        style = DmsTheme.typography.label.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DmsColors.White
                                        )
                                    )
                                }
                            )
                        }

                        // Full-width progress rail
                        DmsProgressRail(
                            progress = project.progress,
                            height = 5.dp
                        )

                        // 4 Phases Distribution
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PhaseItem("Planning", "15%", isComplete = project.progress >= 0.15f)
                            PhaseItem("Development", "45%", isComplete = project.progress >= 0.60f)
                            PhaseItem("Testing", "20%", isComplete = project.progress >= 0.80f)
                            PhaseItem("Deployment", "20%", isComplete = project.progress >= 0.95f)
                        }
                    }
                }
            }

            // TABS: Overview / Tasks / Activity / Files
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf("Overview", "Tasks", "Activity", "Files")
                    items(tabs) { tab ->
                        DmsFilterCapsule(
                            text = tab,
                            isSelected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }

            when (selectedTab) {
                "Overview" -> {
                    // MILESTONES (Horizontal Timeline)
                    item {
                        DmsCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DmsRadii.ShapeR18,
                            backgroundColor = DmsColors.Surface01,
                            padding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Milestones",
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )
                                if (project.milestones.isEmpty()) {
                                    Text(
                                        text = "No milestones defined yet",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                } else {
                                    DmsMilestoneTimeline(milestones = project.milestones)
                                }
                            }
                        }
                    }

                    // TASK SUMMARY & ASSIGNED AGENTS
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Task Summary Card
                            DmsCard(
                                modifier = Modifier.weight(1f),
                                shape = DmsRadii.ShapeR18,
                                backgroundColor = DmsColors.Surface01,
                                padding = 12.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Tasks",
                                        style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val totalCount = projectTasks.size
                                        val doneCount = projectTasks.count { it.status == TaskStatus.DONE }
                                        val pendingCount = totalCount - doneCount
                                        val ratio = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

                                        DmsProgressRing(
                                            progress = ratio,
                                            size = 48.dp,
                                            strokeWidth = 5.dp,
                                            centerContent = {
                                                Text(
                                                    text = "$totalCount",
                                                    style = DmsTheme.typography.caption.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = DmsColors.White
                                                    )
                                                )
                                            }
                                        )
                                        Column {
                                            Text(
                                                text = "$doneCount Done",
                                                style = DmsTheme.typography.caption.copy(
                                                    color = DmsColors.White,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Text(
                                                text = "$pendingCount Pending",
                                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                            )
                                        }
                                    }
                                }
                            }

                            // Assigned Agents Card (Derived strictly from real task assignments)
                            DmsCard(
                                modifier = Modifier.weight(1f),
                                shape = DmsRadii.ShapeR18,
                                backgroundColor = DmsColors.Surface01,
                                padding = 12.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Assigned Agents",
                                        style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (project.assignedAgents.isEmpty()) {
                                        Text(
                                            text = "No agents assigned",
                                            style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                        )
                                    } else {
                                        project.assignedAgents.forEach { agentName ->
                                            Text(
                                                text = "• $agentName",
                                                style = DmsTheme.typography.caption.copy(
                                                    color = DmsColors.White92,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // BLOCKERS
                    if (project.blockers.isNotEmpty()) {
                        item {
                            DmsCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = DmsRadii.ShapeR14,
                                backgroundColor = DmsColors.Surface02,
                                padding = 12.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = "Blocker",
                                        tint = DmsColors.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Active Blocker (${project.blockers.first().duration})",
                                            style = DmsTheme.typography.label.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = DmsColors.White
                                            )
                                        )
                                        Text(
                                            text = project.blockers.first().description,
                                            style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RECENT ACTIVITY FEED
                    if (project.activities.isNotEmpty()) {
                        item {
                            DmsCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = DmsRadii.ShapeR18,
                                backgroundColor = DmsColors.Surface01,
                                padding = 14.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Recent Activity",
                                        style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                    )

                                    project.activities.forEach { activity ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = activity.title,
                                                    style = DmsTheme.typography.bodySmall.copy(
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = DmsColors.White92
                                                    )
                                                )
                                                Text(
                                                    text = "${activity.author} • ${activity.hash ?: ""}",
                                                    style = DmsTheme.typography.caption.copy(
                                                        fontSize = 10.sp,
                                                        color = DmsColors.White48
                                                    )
                                                )
                                            }
                                            Text(
                                                text = activity.timestamp,
                                                style = DmsTheme.typography.caption.copy(
                                                    fontSize = 9.5.sp,
                                                    color = DmsColors.White32
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // NEXT ACTIONS TIMELINE (Derived strictly from real pending project tasks)
                    item {
                        DmsCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DmsRadii.ShapeR18,
                            backgroundColor = DmsColors.Surface01,
                            padding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Next Actions",
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )

                                val pendingTasks = projectTasks.filter { it.status != TaskStatus.DONE }
                                if (pendingTasks.isEmpty()) {
                                    Text(
                                        text = "No upcoming actions",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                } else {
                                    val nextActions = pendingTasks.take(4).mapIndexed { index, task ->
                                        TimeRailItem(
                                            time = task.dueTime,
                                            title = task.title,
                                            isCurrent = index == 0,
                                            agent = task.assignedAgent
                                        )
                                    }
                                    DmsVerticalTimeRail(items = nextActions)
                                }
                            }
                        }
                    }
                }

                "Tasks" -> {
                    item {
                        DmsCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DmsRadii.ShapeR18,
                            backgroundColor = DmsColors.Surface01,
                            padding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Project Tasks (${projectTasks.size})",
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )

                                if (projectTasks.isEmpty()) {
                                    Text(
                                        text = "0 tasks — no tasks created for this project yet.",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                } else {
                                    projectTasks.forEach { task ->
                                        ProjectTaskItem(
                                            task = task,
                                            onToggle = {
                                                coroutineScope.launch {
                                                    taskRepository?.toggleTask(task.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Activity" -> {
                    item {
                        DmsCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DmsRadii.ShapeR18,
                            backgroundColor = DmsColors.Surface01,
                            padding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Live Activity & Commit Stream",
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )
                                if (project.activities.isEmpty()) {
                                    Text(
                                        text = "No recent activity recorded for this project.",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                } else {
                                    project.activities.forEach { activity ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(DmsRadii.ShapeR10)
                                                .background(DmsColors.Surface02)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = activity.title,
                                                style = DmsTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DmsColors.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Author: ${activity.author} • Commit ${activity.hash ?: ""}",
                                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                                )
                                                Text(
                                                    text = activity.timestamp,
                                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Files" -> {
                    item {
                        DmsCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = DmsRadii.ShapeR18,
                            backgroundColor = DmsColors.Surface01,
                            padding = 14.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Repository Contents",
                                        style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                    )
                                    if (filesState is RepositoryFilesState.Loaded) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    repositoryFilesRepository?.refresh()
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = "Refresh",
                                                tint = DmsColors.White64,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                when (val state = filesState) {
                                    is RepositoryFilesState.NotLinked -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(DmsRadii.ShapeR12)
                                                .background(DmsColors.Surface02)
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = DmsColors.White64,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Repository not linked",
                                                style = DmsTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = DmsColors.White
                                                )
                                            )
                                            Text(
                                                text = "This project is not linked to a GitHub repository.",
                                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                            )
                                            onConnectGitHubClick?.let {
                                                DmsPrimaryButton(
                                                    text = "Link GitHub Repository",
                                                    onClick = it,
                                                    modifier = Modifier.height(36.dp)
                                                )
                                            }
                                        }
                                    }
                                    is RepositoryFilesState.Disconnected -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(DmsRadii.ShapeR12)
                                                .background(DmsColors.Surface02)
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = null,
                                                tint = DmsColors.White64,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Repository files unavailable — connect GitHub",
                                                style = DmsTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = DmsColors.White
                                                )
                                            )
                                            Text(
                                                text = "Sign in to browse live repository tree and branches.",
                                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                            )
                                            onConnectGitHubClick?.let {
                                                DmsPrimaryButton(
                                                    text = "Connect GitHub",
                                                    onClick = it,
                                                    modifier = Modifier.height(36.dp)
                                                )
                                            }
                                        }
                                    }
                                    is RepositoryFilesState.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = DmsColors.White,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    is RepositoryFilesState.Error -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(DmsRadii.ShapeR12)
                                                .background(DmsColors.Surface02)
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Failed to load repository files",
                                                style = DmsTheme.typography.bodySmall.copy(
                                                    color = DmsColors.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Text(
                                                text = state.message,
                                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                            )
                                            DmsPrimaryButton(
                                                text = "Retry",
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repositoryFilesRepository?.refresh()
                                                    }
                                                },
                                                modifier = Modifier.height(32.dp)
                                            )
                                        }
                                    }
                                    is RepositoryFilesState.Loaded -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(DmsRadii.ShapeR12)
                                                .background(DmsColors.Surface02)
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Navigation header & Breadcrumbs
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (state.path.isNotBlank()) {
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                repositoryFilesRepository?.navigateUp()
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                            contentDescription = "Up",
                                                            tint = DmsColors.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = if (state.path.isBlank()) "${state.repository} (${state.branch})" else "${state.repository} / ${state.path}",
                                                    style = DmsTheme.typography.caption.copy(
                                                        color = DmsColors.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                            }

                                            if (state.entries.isEmpty()) {
                                                Text(
                                                    text = "Empty directory",
                                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48),
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            } else {
                                                state.entries.forEach { entry ->
                                                    RepositoryFileItem(
                                                        entry = entry,
                                                        onClick = {
                                                            if (entry.isDirectory) {
                                                                coroutineScope.launch {
                                                                    repositoryFilesRepository?.navigateTo(entry.name)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun RepositoryFileItem(
    entry: RepositoryFileEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DmsRadii.ShapeR8)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                contentDescription = null,
                tint = if (entry.isDirectory) DmsColors.White else DmsColors.White64,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = entry.name,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    color = if (entry.isDirectory) DmsColors.White else DmsColors.White80,
                    fontWeight = if (entry.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 1
            )
        }

        if (!entry.isDirectory && entry.size > 0) {
            Text(
                text = formatFileSize(entry.size),
                style = DmsTheme.typography.caption.copy(
                    fontSize = 9.sp,
                    color = DmsColors.White32
                )
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
    }
}

@Composable
private fun ProjectTaskItem(
    task: Task,
    onToggle: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DmsRadii.ShapeR12)
            .background(DmsColors.Surface02)
            .border(BorderStroke(1.dp, DmsColors.White10), DmsRadii.ShapeR12)
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(DmsRadii.ShapeR8)
                .background(if (isDone) DmsColors.White else DmsColors.Surface02)
                .border(
                    BorderStroke(1.dp, if (isDone) DmsColors.White else DmsColors.White32),
                    DmsRadii.ShapeR8
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Completed",
                    tint = DmsColors.OledBlack,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = DmsTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) DmsColors.White48 else DmsColors.White
                )
            )
            task.description?.let { desc ->
                Text(
                    text = desc,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        color = DmsColors.White48
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.assignedAgent?.let { agent ->
                    Text(
                        text = "Assigned: $agent",
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 9.sp,
                            color = DmsColors.White64
                        )
                    )
                }
                Text(
                    text = if (isDone) "Completed: ${task.completedAt ?: task.dueTime}" else "Due: ${task.dueTime}",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White48
                    )
                )
            }
        }

        DmsStatusCapsule(
            text = task.priority.name,
            height = 20.dp,
            borderColor = DmsColors.White20
        )
    }
}

@Composable
private fun PhaseItem(name: String, percentage: String, isComplete: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = percentage,
            style = DmsTheme.typography.caption.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isComplete) DmsColors.White else DmsColors.White48
            )
        )
        Text(
            text = name,
            style = DmsTheme.typography.caption.copy(
                fontSize = 9.sp,
                color = DmsColors.White32
            )
        )
    }
}
