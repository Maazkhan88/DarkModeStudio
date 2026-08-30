package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

data class MilestoneItem(
    val title: String,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false,
    val date: String? = null
)

@Composable
fun DmsMilestoneTimeline(
    milestones: List<MilestoneItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        milestones.forEachIndexed { index, milestone ->
            val nodeStyle = when {
                milestone.isCompleted -> NodeStyle.CHECK
                milestone.isActive -> NodeStyle.DOUBLE_RING
                else -> NodeStyle.DOTTED
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left connecting rail
                    if (index > 0) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterStart)
                                .height(2.dp)
                        ) {
                            drawLine(
                                color = if (milestone.isCompleted || milestone.isActive) DmsColors.White else DmsColors.White14,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // Right connecting rail
                    if (index < milestones.size - 1) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterEnd)
                                .height(2.dp)
                        ) {
                            val isConnected = milestone.isCompleted && milestones[index + 1].let { it.isCompleted || it.isActive }
                            drawLine(
                                color = if (isConnected) DmsColors.White else DmsColors.White14,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // Node in center
                    DmsNode(
                        style = nodeStyle,
                        size = 12.dp,
                        color = if (milestone.isCompleted || milestone.isActive) DmsColors.White else DmsColors.White48
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = milestone.title,
                    style = DmsTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        fontWeight = if (milestone.isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (milestone.isActive) DmsColors.White else DmsColors.White48
                    ),
                    maxLines = 1
                )

                if (milestone.date != null) {
                    Text(
                        text = milestone.date,
                        style = DmsTheme.typography.caption.copy(
                            fontSize = 9.sp,
                            color = DmsColors.White32
                        )
                    )
                }
            }
        }
    }
}

data class TimeRailItem(
    val time: String,
    val title: String,
    val subtitle: String? = null,
    val isCurrent: Boolean = false,
    val isDone: Boolean = false,
    val tag: String? = null,
    val agent: String? = null
)

@Composable
fun DmsVerticalTimeRail(
    items: List<TimeRailItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (index < items.size - 1) 16.dp else 0.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time column
                Text(
                    text = item.time,
                    style = DmsTheme.typography.caption.copy(
                        color = if (item.isCurrent) DmsColors.White else DmsColors.White48,
                        fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    modifier = Modifier.width(44.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Vertical rail & Node
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(16.dp)
                ) {
                    val nodeStyle = when {
                        item.isDone -> NodeStyle.CHECK
                        item.isCurrent -> NodeStyle.SOLID
                        else -> NodeStyle.HOLLOW
                    }

                    DmsNode(
                        style = nodeStyle,
                        size = if (item.isCurrent) 10.dp else 8.dp,
                        color = if (item.isCurrent || item.isDone) DmsColors.White else DmsColors.White32
                    )

                    if (index < items.size - 1) {
                        Canvas(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                        ) {
                            drawLine(
                                color = DmsColors.White20,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = DmsTheme.typography.bodySmall.copy(
                                color = if (item.isCurrent) DmsColors.White else DmsColors.White92,
                                fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )

                        if (item.isCurrent) {
                            DmsStatusCapsule(
                                text = "NOW",
                                height = 20.dp,
                                borderColor = DmsColors.White64
                            )
                        }
                    }

                    if (item.subtitle != null) {
                        Text(
                            text = item.subtitle,
                            style = DmsTheme.typography.caption.copy(
                                color = DmsColors.White48
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (item.agent != null) {
                        Text(
                            text = "Assigned: ${item.agent}",
                            style = DmsTheme.typography.caption.copy(
                                color = DmsColors.White32,
                                fontSize = 9.5.sp
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
