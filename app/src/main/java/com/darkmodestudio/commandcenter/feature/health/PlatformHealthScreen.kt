package com.darkmodestudio.commandcenter.feature.health

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsHeroCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsNode
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsProgressRing
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.IntegrationHealth
import com.darkmodestudio.commandcenter.core.model.IntegrationItem

@Composable
fun PlatformHealthScreen(
    healthRepository: HealthRepository,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onConnectServiceClick: ((String) -> Unit)? = null,
    onSyncNowClick: (() -> Unit)? = null
) {
    val integrations by healthRepository.integrations.collectAsState(initial = emptyList())
    val summary = healthRepository.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "platform health",
            onNotificationClick = onNotificationClick,
            onAvatarClick = onAvatarClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO CARD: Platform Health ● Live + 96% Ring (94dp)
            item {
                DmsHeroCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR22,
                    backgroundColor = DmsColors.Surface01,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DmsNode(style = NodeStyle.SOLID, size = 6.dp, color = DmsColors.White)
                                Text(
                                    text = "Platform Health",
                                    style = DmsTheme.typography.h2
                                )
                                DmsStatusCapsule(
                                    text = "LIVE",
                                    height = 20.dp,
                                    borderColor = DmsColors.White48
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HealthCountCol(count = "${summary.connectedCount}", label = "Connected")
                                HealthCountCol(count = "${summary.degradedCount}", label = "Degraded")
                                HealthCountCol(count = "${summary.disconnectedCount}", label = "Disconnected")
                                HealthCountCol(count = "${summary.alertsCount}", label = "Alerts")
                            }
                        }

                        // Large 94dp Progress Ring
                        DmsProgressRing(
                            progress = summary.healthScore,
                            size = 94.dp,
                            strokeWidth = 8.dp,
                            centerContent = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(summary.healthScore * 100).toInt()}%",
                                        style = DmsTheme.typography.h2.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = DmsColors.White
                                        )
                                    )
                                    Text(
                                        text = "HEALTH",
                                        style = DmsTheme.typography.caption.copy(
                                            fontSize = 8.sp,
                                            letterSpacing = 1.sp,
                                            color = DmsColors.White48
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // INTEGRATION CARDS
            items(integrations) { item ->
                IntegrationDetailCard(
                    item = item,
                    onClick = { onConnectServiceClick?.invoke(item.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun HealthCountCol(count: String, label: String) {
    Column {
        Text(
            text = count,
            style = DmsTheme.typography.label.copy(
                fontSize = 14.sp,
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

@Composable
private fun IntegrationDetailCard(
    item: IntegrationItem,
    onClick: (() -> Unit)? = null
) {
    val nodeStyle = when (item.health) {
        IntegrationHealth.OPERATIONAL -> NodeStyle.SOLID
        IntegrationHealth.DEGRADED -> NodeStyle.DOUBLE_RING
        IntegrationHealth.DISCONNECTED -> NodeStyle.SLASH
        IntegrationHealth.ALERT -> NodeStyle.DOTTED
    }

    DmsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = DmsRadii.ShapeR18,
        backgroundColor = DmsColors.Surface01,
        padding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Icon, Name + Category, Status Capsule
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
                            imageVector = Icons.Outlined.CloudQueue,
                            contentDescription = null,
                            tint = DmsColors.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.name,
                            style = DmsTheme.typography.h4.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "${item.category} • Synced ${item.lastSync}",
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = DmsColors.White48
                            )
                        )
                    }
                }

                DmsStatusCapsule(
                    text = item.health.displayName,
                    nodeStyle = nodeStyle,
                    height = 24.dp
                )
            }

            // Active Alert Warning (if degraded)
            if (item.activeAlerts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DmsRadii.ShapeR8)
                        .background(DmsColors.Surface02)
                        .border(BorderStroke(1.dp, DmsColors.White20), DmsRadii.ShapeR8)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = DmsColors.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.activeAlerts.first(),
                            style = DmsTheme.typography.caption.copy(
                                fontSize = 10.sp,
                                color = DmsColors.White80
                            )
                        )
                    }
                }
            }

            // Metrics Grid (2x2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DmsRadii.ShapeR10)
                    .background(DmsColors.Surface02)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chunks = item.metrics.chunked(2)
                chunks.forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowMetrics.forEach { metric ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = metric.label,
                                    style = DmsTheme.typography.caption.copy(
                                        fontSize = 9.sp,
                                        color = DmsColors.White48
                                    )
                                )
                                Text(
                                    text = metric.value,
                                    style = DmsTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DmsColors.White92
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
