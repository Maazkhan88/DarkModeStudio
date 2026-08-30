package com.darkmodestudio.commandcenter.feature.agents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
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
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRail
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryOutlineButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsUsageLineGraph
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Agent

@Composable
fun AgentsScreen(
    agentRepository: AgentRepository,
    onManageAgentsClick: () -> Unit = {},
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val agents by agentRepository.agents.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "agents",
            onNotificationClick = onNotificationClick,
            onAvatarClick = onAvatarClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Subtitle + Top Right "Manage Agents"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Agents",
                            style = DmsTheme.typography.displayL
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Coding agents & local session usage",
                            style = DmsTheme.typography.bodySmall.copy(color = DmsColors.White64)
                        )
                    }

                    DmsSecondaryOutlineButton(
                        text = "Manage Agents",
                        onClick = onManageAgentsClick,
                        height = 36.dp
                    )
                }
            }

            // USAGE SUMMARY CARD (Local Session Tracking with transparent label)
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR20,
                    backgroundColor = DmsColors.Surface01,
                    padding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Local Session Metrics",
                                style = DmsTheme.typography.h4.copy(fontSize = 15.sp)
                            )
                            Text(
                                text = "Quota telemetry: Local session",
                                style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1.1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AllocationMetricRow(
                                    label = "DMS Runs",
                                    used = "${agentRepository.totalRunsUsed}",
                                    total = "/ 1,500"
                                )
                                AllocationMetricRow(
                                    label = "Messages",
                                    used = "8,620",
                                    total = "/ 20,000"
                                )
                                AllocationMetricRow(
                                    label = "Tasks",
                                    used = "${agentRepository.totalTasksUsed}",
                                    total = "/ 600"
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Technical 2dp line graph with dotted grid
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(84.dp)
                            ) {
                                DmsUsageLineGraph(dataPoints = agentRepository.globalHistory)
                            }
                        }
                    }
                }
            }

            // AGENT CARDS
            items(agents) { agent ->
                AgentCardItem(agent = agent)
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun AllocationMetricRow(
    label: String,
    used: String,
    total: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = DmsTheme.typography.caption.copy(
                fontSize = 10.sp,
                color = DmsColors.White48
            ),
            modifier = Modifier.width(58.dp)
        )
        Text(
            text = used,
            style = DmsTheme.typography.label.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DmsColors.White
            )
        )
        Text(
            text = total,
            style = DmsTheme.typography.caption.copy(
                fontSize = 10.sp,
                color = DmsColors.White32
            )
        )
    }
}

@Composable
private fun AgentCardItem(agent: Agent) {
    DmsCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DmsRadii.ShapeR18,
        backgroundColor = DmsColors.Surface01,
        padding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Icon, Name + Provider, Mode/Speed, Status Capsule
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
                        Icon(
                            imageVector = Icons.Outlined.Memory,
                            contentDescription = null,
                            tint = DmsColors.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = agent.name,
                                style = DmsTheme.typography.h4.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            )
                            DmsStatusCapsule(
                                text = "${agent.mode} • ${agent.speed}",
                                height = 20.dp,
                                borderColor = DmsColors.White20
                            )
                        }
                        Text(
                            text = "${agent.provider.displayName} • Local Session",
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = DmsColors.White48
                            )
                        )
                    }
                }

                DmsStatusCapsule(
                    text = agent.statusText,
                    nodeStyle = NodeStyle.SOLID,
                    height = 24.dp
                )
            }

            // Current Task
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DmsRadii.ShapeR10)
                    .background(DmsColors.Surface02)
                    .padding(8.dp)
            ) {
                Text(
                    text = "Current Task",
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 9.sp,
                        color = DmsColors.White48
                    )
                )
                Text(
                    text = agent.currentTask,
                    style = DmsTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = DmsColors.White92
                    ),
                    maxLines = 1
                )
            }

            // Metrics: Runs, Messages, Tasks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgentSubMetric(label = "Runs", used = "${agent.runsUsed}", total = "${agent.runsTotal}")
                AgentSubMetric(label = "Messages", used = "${agent.messagesUsed}", total = "${agent.messagesTotal}")
                AgentSubMetric(label = "Tasks", used = "${agent.tasksUsed}", total = "${agent.tasksTotal}")
            }

            // Progress Rail
            DmsProgressRail(
                progress = agent.usagePercentage,
                height = 2.dp
            )
        }
    }
}

@Composable
private fun AgentSubMetric(label: String, used: String, total: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$used / $total",
            style = DmsTheme.typography.caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DmsColors.White
            )
        )
        Text(
            text = label,
            style = DmsTheme.typography.caption.copy(
                fontSize = 9.sp,
                color = DmsColors.White48
            )
        )
    }
}
