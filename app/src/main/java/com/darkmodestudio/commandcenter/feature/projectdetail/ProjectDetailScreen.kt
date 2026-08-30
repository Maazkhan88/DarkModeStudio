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
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.ProjectRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsHeroCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsMilestoneTimeline
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

import androidx.compose.runtime.collectAsState

@Composable
fun ProjectDetailScreen(
    projectId: String,
    projectRepository: ProjectRepository,
    onBackClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val projectFlow by projectRepository.getProjectFlow(projectId).collectAsState(initial = null)
    val projectsList by projectRepository.projects.collectAsState(initial = emptyList())
    val project = projectFlow ?: projectsList.find { it.id == projectId } ?: projectsList.firstOrNull() ?: return

    var selectedTab by remember { mutableStateOf("Overview") }

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
            // HERO CARD: SecondMe MVP + 54% Progress Ring + Full Rail + Phase Distribution
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                            // 72dp Progress Ring with 54% in Center
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

                        // 4 Phases Distribution: Planning (15%), Development (45%), Testing (20%), Deployment (20%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PhaseItem("Planning", "15%", isComplete = true)
                            PhaseItem("Development", "45%", isComplete = true)
                            PhaseItem("Testing", "20%", isComplete = false)
                            PhaseItem("Deployment", "20%", isComplete = false)
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
                        DmsMilestoneTimeline(milestones = project.milestones)
                    }
                }
            }

            // TASK SUMMARY & ASSIGNED AGENTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Task Summary Card (Ring with 12 Total, 6 Done, 6 Pending)
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
                                DmsProgressRing(
                                    progress = project.doneTasks.toFloat() / project.totalTasks,
                                    size = 48.dp,
                                    strokeWidth = 5.dp,
                                    centerContent = {
                                        Text(
                                            text = "${project.totalTasks}",
                                            style = DmsTheme.typography.caption.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DmsColors.White
                                            )
                                        )
                                    }
                                )
                                Column {
                                    Text(
                                        text = "${project.doneTasks} Done",
                                        style = DmsTheme.typography.caption.copy(
                                            color = DmsColors.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "${project.pendingTasks} Pending",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                }
                            }
                        }
                    }

                    // Assigned Agents Card (Codex, Claude, Antigravity)
                    DmsCard(
                        modifier = Modifier.weight(1f),
                        shape = DmsRadii.ShapeR18,
                        backgroundColor = DmsColors.Surface01,
                        padding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Agents",
                                style = DmsTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
                            )
                            AgentUsageRow(name = "Codex", percent = 0.68f)
                            AgentUsageRow(name = "Claude", percent = 0.82f)
                            AgentUsageRow(name = "Antigravity", percent = 0.41f)
                        }
                    }
                }
            }

            // BLOCKERS (Compact warning card)
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
                                        fontSize = 10.sp,
                                        color = DmsColors.White32
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // NEXT ACTIONS TIMELINE (Vertical rail with nodes)
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

                        val nextActions = listOf(
                            TimeRailItem(
                                time = "Today",
                                title = "Merge PR #342 Memory Vector indexing",
                                isCurrent = true,
                                agent = "Codex"
                            ),
                            TimeRailItem(
                                time = "Tomorrow",
                                title = "Staging deployment smoke tests",
                                isCurrent = false,
                                agent = "Claude"
                            ),
                            TimeRailItem(
                                time = "Sep 05",
                                title = "User auth edge benchmark review",
                                isCurrent = false,
                                agent = "Antigravity"
                            )
                        )

                        DmsVerticalTimeRail(items = nextActions)
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

@Composable
private fun AgentUsageRow(name: String, percent: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White80
                )
            )
            Text(
                text = "${(percent * 100).toInt()}%",
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White
                )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        DmsProgressRail(progress = percent, height = 2.dp)
    }
}
