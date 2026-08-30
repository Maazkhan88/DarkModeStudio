package com.darkmodestudio.commandcenter.feature.projects

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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRail
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Project
import com.darkmodestudio.commandcenter.core.model.ProjectStatus

@Composable
fun ProjectsScreen(
    projectRepository: ProjectRepository,
    onProjectClick: (String) -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val projects by projectRepository.projects.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All") }

    val activeCount = projects.count { it.status == ProjectStatus.ON_TRACK || it.status == ProjectStatus.IN_PROGRESS }
    val doneCount = projects.count { it.status == ProjectStatus.DONE }
    val blockedCount = projects.count { it.status == ProjectStatus.BLOCKED || it.status == ProjectStatus.WAITING }

    val filteredProjects = remember(projects, selectedFilter) {
        when (selectedFilter) {
            "Active" -> projects.filter { it.status == ProjectStatus.ON_TRACK || it.status == ProjectStatus.IN_PROGRESS }
            "Waiting" -> projects.filter { it.status == ProjectStatus.WAITING || it.status == ProjectStatus.BLOCKED }
            "Done" -> projects.filter { it.status == ProjectStatus.DONE }
            else -> projects
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "projects",
            onNotificationClick = onNavigateToUpdates,
            onAvatarClick = onNavigateToSettings
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header & Subheading
            item {
                Column {
                    Text(
                        text = "Projects",
                        style = DmsTheme.typography.displayL
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "«All systems. Every project. One mission.»",
                        style = DmsTheme.typography.bodySmall.copy(color = DmsColors.White64)
                    )
                }
            }

            // Top Summary (Dynamic Counts)
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR16,
                    backgroundColor = DmsColors.Surface01,
                    padding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryColumn(
                            value = "$activeCount",
                            label = "Active",
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = "Active" }
                        )
                        Divider()
                        SummaryColumn(
                            value = "$doneCount",
                            label = "Completed",
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = "Done" }
                        )
                        Divider()
                        SummaryColumn(
                            value = "$blockedCount",
                            label = "Blocked",
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = "Waiting" }
                        )
                    }
                }
            }

            // Filters: All / Active / Waiting / Done
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "Active", "Waiting", "Done")
                    items(filters) { filter ->
                        DmsFilterCapsule(
                            text = filter,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }

            // Project Cards
            items(filteredProjects) { project ->
                ProjectItemCard(
                    project = project,
                    onClick = { onProjectClick(project.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = DmsTheme.typography.h2.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = label,
            style = DmsTheme.typography.caption.copy(
                fontSize = 10.sp,
                color = DmsColors.White48
            )
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(DmsColors.White14)
    )
}

@Composable
private fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit
) {
    DmsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = DmsRadii.ShapeR18,
        backgroundColor = DmsColors.Surface01,
        padding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Icon, Title & Status Capsule, Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                color = DmsColors.White
                            )
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = project.name,
                                style = DmsTheme.typography.h4.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
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
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = DmsColors.White48
                            ),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DmsStatusCapsule(
                        text = project.status.displayName,
                        nodeStyle = project.status.nodeStyle,
                        height = 26.dp
                    )
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = DmsColors.White48,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Row 2: Completion Progress Rail & %
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completion",
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 10.sp,
                            color = DmsColors.White48
                        )
                    )
                    Text(
                        text = "${(project.progress * 100).toInt()}%",
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
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

            // Row 3: Metadata (Next Milestone, Due Date, and exact timestamp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next: ${project.nextMilestone}",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        color = DmsColors.White64
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "${project.lastUpdate}",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.5.sp,
                        color = DmsColors.White32
                    )
                )
            }
        }
    }
}
