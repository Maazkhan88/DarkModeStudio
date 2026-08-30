package com.darkmodestudio.commandcenter.feature.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.AgentRepository
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.data.repository.TaskRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsHeroCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsNode
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsOrbitalCanvas
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRail
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRing
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Project
import com.darkmodestudio.commandcenter.core.model.ProjectStatus
import com.darkmodestudio.commandcenter.core.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    projectRepository: ProjectRepository,
    taskRepository: TaskRepository,
    agentRepository: AgentRepository,
    healthRepository: HealthRepository,
    onNavigateToProject: (String) -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToExecution: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onManualSync: (() -> Unit)? = null
) {
    val projects by projectRepository.projects.collectAsState(initial = emptyList())
    val tasks by taskRepository.tasks.collectAsState(initial = emptyList())
    val agents by agentRepository.agents.collectAsState(initial = emptyList())
    val integrations by healthRepository.integrations.collectAsState(initial = emptyList())

    val todayDateFormatted = SimpleDateFormat("EEEE, MMM dd", Locale.US).format(Date())

    val doneTasksCount = tasks.count { it.status == TaskStatus.DONE }
    val totalTasksCount = tasks.size
    val pendingTasksCount = tasks.count { it.status == TaskStatus.PENDING }
    val blockedTasksCount = tasks.count { it.status == TaskStatus.BLOCKED }
    val activeProjectsCount = projects.count { it.status == ProjectStatus.ON_TRACK || it.status == ProjectStatus.IN_PROGRESS }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "command center",
            onNotificationClick = onNavigateToUpdates,
            onAvatarClick = onNavigateToSettings,
            hasNotifications = true
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO — TODAY (Tap triggers real manual cloud sync)
            item {
                DmsHeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onManualSync?.invoke() },
                    shape = DmsRadii.ShapeR22,
                    backgroundColor = DmsColors.Surface01,
                    padding = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today",
                                style = DmsTheme.typography.displayL.copy(fontSize = 32.sp)
                            )
                            Text(
                                text = todayDateFormatted,
                                style = DmsTheme.typography.bodySmall.copy(color = DmsColors.White64),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "«Ship daily. Iterate faster.\nCompound forever.»",
                                style = DmsTheme.typography.body.copy(
                                    color = DmsColors.White80,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }

                        // Technical Orbital Visual Graphic
                        DmsOrbitalCanvas(
                            modifier = Modifier.size(110.dp),
                            size = 110.dp
                        )
                    }
                }
            }

            // HERO METRICS (4 equal interactive metrics)
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR16,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroMetricItem(
                            title = "Focus Score",
                            value = "94",
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToUpdates)
                        )
                        MetricDivider()
                        HeroMetricItem(
                            title = "Deep Work",
                            value = "4.2h",
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToExecution)
                        )
                        MetricDivider()
                        HeroMetricItem(
                            title = "Tasks Done",
                            value = "$doneTasksCount/$totalTasksCount",
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToExecution)
                        )
                        MetricDivider()
                        HeroMetricItem(
                            title = "Projects Active",
                            value = "$activeProjectsCount",
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onNavigateToProjects)
                        )
                    }
                }
            }

            // PROJECTS PREVIEW
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR20,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Projects",
                                style = DmsTheme.typography.h3
                            )
                            Row(
                                modifier = Modifier.clickable(onClick = onNavigateToProjects),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "View all",
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = DmsColors.White64,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Real Project Rows
                        projects.take(4).forEach { project ->
                            HomeProjectRow(
                                project = project,
                                onClick = { onNavigateToProject(project.id) }
                            )
                        }
                    }
                }
            }

            // INTEGRATIONS PREVIEW
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Integrations",
                            style = DmsTheme.typography.h3
                        )
                        Row(
                            modifier = Modifier.clickable(onClick = onNavigateToHealth),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DmsNode(style = NodeStyle.SOLID, size = 5.dp, color = DmsColors.White)
                            Text(
                                text = "All Systems Operational →",
                                style = DmsTheme.typography.caption.copy(
                                    color = DmsColors.White80,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(integrations) { item ->
                            HomeIntegrationTile(
                                name = item.name,
                                status = item.health.displayName,
                                onClick = onNavigateToHealth
                            )
                        }
                    }
                }
            }

            // CODING AGENTS PREVIEW
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coding Agents",
                            style = DmsTheme.typography.h3
                        )
                        Text(
                            text = "Manage →",
                            style = DmsTheme.typography.caption.copy(color = DmsColors.White64),
                            modifier = Modifier.clickable(onClick = onNavigateToAgents)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        agents.take(3).forEach { agent ->
                            HomeAgentCompactCard(
                                name = agent.name,
                                usagePercentage = agent.usagePercentage,
                                remainingText = "${agent.runsUsed}/${agent.runsTotal}",
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToAgents
                            )
                        }
                    }
                }
            }

            // TASKS + REMINDER (Dynamic Counts & Exact Dates)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tasks Summary Card
                    DmsCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToExecution),
                        shape = DmsRadii.ShapeR16,
                        backgroundColor = DmsColors.Surface01,
                        padding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = DmsColors.White80,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Tasks",
                                    style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Text(
                                text = "$doneTasksCount Done",
                                style = DmsTheme.typography.caption.copy(color = DmsColors.White)
                            )
                            Text(
                                text = "$pendingTasksCount Pending",
                                style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
                            )
                            Text(
                                text = "$blockedTasksCount Blocked",
                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                            )
                        }
                    }

                    // Reminder Card
                    DmsCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToUpdates),
                        shape = DmsRadii.ShapeR16,
                        backgroundColor = DmsColors.Surface01,
                        padding = 12.dp
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.height(86.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = DmsColors.White80,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Reminder",
                                        style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Daily standup",
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White92),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Aug 30 • 05:00 PM",
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                )
                            }

                            // Page dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(DmsColors.White)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(DmsColors.White20)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(DmsColors.White20)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom space for floating navigation bar
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun HeroMetricItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = DmsTheme.typography.h3.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = DmsTheme.typography.caption.copy(
                fontSize = 10.sp,
                color = DmsColors.White48
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(DmsColors.White14)
    )
}

@Composable
private fun HomeProjectRow(
    project: Project,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(DmsRadii.ShapeR12)
            .background(DmsColors.Surface02)
            .border(BorderStroke(1.dp, DmsColors.White14), DmsRadii.ShapeR12)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Project square icon 36dp radius 10dp
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(DmsRadii.ShapeR10)
                .background(DmsColors.SurfaceSelected)
                .border(BorderStroke(1.dp, DmsColors.White20), DmsRadii.ShapeR10),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = project.iconTag,
                style = DmsTheme.typography.label.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = DmsColors.White
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title and Subtitle
        Column(modifier = Modifier.width(90.dp)) {
            Text(
                text = project.name,
                style = DmsTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DmsColors.White
                ),
                maxLines = 1
            )
            Text(
                text = project.nextMilestone,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 9.5.sp,
                    color = DmsColors.White48
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Center progress rail & Percentage
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White32
                    )
                )
                Text(
                    text = "${(project.progress * 100).toInt()}%",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = DmsColors.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            DmsProgressRail(
                progress = project.progress,
                height = 3.dp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Status capsule
        DmsStatusCapsule(
            text = project.status.displayName,
            nodeStyle = project.status.nodeStyle,
            height = 24.dp
        )
    }
}

@Composable
private fun HomeIntegrationTile(
    name: String,
    status: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(50.dp)
            .clip(DmsRadii.ShapeR14)
            .background(DmsColors.Surface01)
            .border(BorderStroke(1.dp, DmsColors.White14), DmsRadii.ShapeR14)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(DmsRadii.ShapeR8)
                .background(DmsColors.SurfaceSelected),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudQueue,
                contentDescription = null,
                tint = DmsColors.White80,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = name,
                style = DmsTheme.typography.label.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DmsNode(style = NodeStyle.SOLID, size = 3.5.dp, color = DmsColors.White)
                Text(
                    text = status,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White48
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeAgentCompactCard(
    name: String,
    usagePercentage: Float,
    remainingText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    DmsCard(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = DmsRadii.ShapeR16,
        backgroundColor = DmsColors.Surface01,
        padding = 10.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = DmsTheme.typography.label.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                DmsProgressRing(
                    progress = usagePercentage,
                    size = 22.dp,
                    strokeWidth = 3.dp
                )
            }

            Column {
                Text(
                    text = "${(usagePercentage * 100).toInt()}%",
                    style = DmsTheme.typography.h4.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = remainingText,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White48
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                DmsProgressRail(
                    progress = usagePercentage,
                    height = 2.dp
                )
            }
        }
    }
}
