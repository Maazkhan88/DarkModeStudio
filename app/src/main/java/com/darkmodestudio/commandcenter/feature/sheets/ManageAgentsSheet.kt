package com.darkmodestudio.commandcenter.feature.sheets

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.Agent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAgentsSheet(
    agents: List<Agent>,
    onDismiss: () -> Unit,
    onRefreshQuotas: () -> Unit,
    onPairDesktopHost: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DmsColors.OledBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Agent Runtimes",
                        style = DmsTheme.typography.h3
                    )
                    Text(
                        text = "Official CLI runtime sessions via DMS Desktop Host",
                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = DmsColors.White64
                    )
                }
            }

            // Desktop Host & Security Guidance Card
            DmsCard(
                modifier = Modifier.fillMaxWidth(),
                shape = DmsRadii.ShapeR12,
                backgroundColor = DmsColors.Surface02,
                padding = 10.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Computer,
                        contentDescription = null,
                        tint = DmsColors.White64,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Agents run inside your authenticated desktop sessions (ChatGPT, Claude App, Google/keyring). API keys and billing remain strictly separate.",
                        style = DmsTheme.typography.caption.copy(fontSize = 10.sp, color = DmsColors.White64, lineHeight = 14.sp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(agents) { agent ->
                    val authSource = when (agent.id) {
                        "codex" -> "ChatGPT Account (Desktop Runtime)"
                        "claude" -> "Claude App Subscription (Desktop Runtime)"
                        "antigravity" -> "Google Account / agy keyring (Desktop Runtime)"
                        else -> "Local Runtime Session"
                    }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(DmsRadii.ShapeR8)
                                        .background(DmsColors.SurfaceSelected)
                                        .border(BorderStroke(1.dp, DmsColors.White20), DmsRadii.ShapeR8),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Memory,
                                        contentDescription = null,
                                        tint = DmsColors.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = agent.name,
                                        style = DmsTheme.typography.label.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DmsColors.White
                                        )
                                    )
                                    Text(
                                        text = authSource,
                                        style = DmsTheme.typography.caption.copy(
                                            fontSize = 9.5.sp,
                                            color = DmsColors.White48
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            DmsStatusCapsule(
                                text = "Runtime Ready",
                                nodeStyle = NodeStyle.SOLID,
                                height = 24.dp
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DmsSecondaryButton(
                    text = "Pair Desktop Host",
                    onClick = {
                        onPairDesktopHost?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )

                DmsPrimaryButton(
                    text = "Refresh Sessions",
                    onClick = {
                        onRefreshQuotas()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
