package com.darkmodestudio.commandcenter.feature.connectstack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.darkmodestudio.commandcenter.core.auth.ConnectionState
import com.darkmodestudio.commandcenter.core.auth.ProviderCategory
import com.darkmodestudio.commandcenter.core.auth.ProviderDefinition
import com.darkmodestudio.commandcenter.core.auth.ProviderRegistry
import com.darkmodestudio.commandcenter.core.data.repository.HealthRepository
import com.darkmodestudio.commandcenter.core.data.repository.NotificationRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsFilterCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsPrimaryButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTextField
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsToggle
import com.darkmodestudio.commandcenter.core.designsystem.component.NodeStyle
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme
import com.darkmodestudio.commandcenter.core.model.NotificationToggleState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectStackScreen(
    healthRepository: HealthRepository,
    notificationRepository: NotificationRepository,
    onContinueClick: () -> Unit,
    onConnectServiceClick: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val integrations by healthRepository.integrations.collectAsState(initial = emptyList())
    val toggleStates by notificationRepository.toggleStates.collectAsState(initial = NotificationToggleState())

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProviderCategory.ALL) }

    val providers = remember(searchQuery, selectedCategory) {
        val byCat = ProviderRegistry.getProvidersByCategory(selectedCategory)
        if (searchQuery.isBlank()) byCat else ProviderRegistry.searchProviders(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
            .statusBarsPadding()
            .padding(horizontal = DmsSpacing.ScreenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Progress rail with 3 numbered nodes (22dp diameter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepNode(step = "1", isActive = true, isCompleted = true)
            StepConnector(isActive = true)
            StepNode(step = "2", isActive = true, isCompleted = false)
            StepConnector(isActive = false)
            StepNode(step = "3", isActive = false, isCompleted = false)
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Connect your stack",
                        style = DmsTheme.typography.displayL
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "«Sign in with official OAuth & desktop runtime sessions. Dark Mode Studio encrypts all secrets with Android Keystore.»",
                        style = DmsTheme.typography.body.copy(
                            color = DmsColors.White80,
                            lineHeight = 19.sp
                        )
                    )
                }
            }

            // Category Filter Pills
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProviderCategory.values().forEach { cat ->
                        DmsFilterCapsule(
                            text = cat.displayName,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }
            }

            // Search Filter
            item {
                DmsTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search providers, AI agents, cloud...",
                    label = "Filter Stack",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = DmsColors.White48,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Provider Cards dynamically sourced from ProviderRegistry
            items(providers) { provider ->
                val integration = integrations.find { it.id.equals(provider.id, ignoreCase = true) }
                val isConnected = integration?.isConnected == true

                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR16,
                    backgroundColor = DmsColors.Surface01,
                    padding = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
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
                                Text(
                                    text = provider.iconTag,
                                    style = DmsTheme.typography.label.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DmsColors.White,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = provider.displayName,
                                    style = DmsTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        color = DmsColors.White
                                    )
                                )
                                Text(
                                    text = provider.description,
                                    style = DmsTheme.typography.caption.copy(
                                        fontSize = 9.5.sp,
                                        color = DmsColors.White48
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isConnected) {
                            DmsStatusCapsule(
                                text = "Connected",
                                nodeStyle = NodeStyle.SOLID,
                                height = 28.dp,
                                modifier = Modifier.clickable {
                                    onConnectServiceClick?.invoke(provider.id)
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(DmsRadii.ShapeR8)
                                    .background(DmsColors.White)
                                    .clickable {
                                        onConnectServiceClick?.invoke(provider.id)
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Connect",
                                    style = DmsTheme.typography.label.copy(
                                        color = DmsColors.OledBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Keystore Encryption Notice
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
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = DmsColors.White80,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Encrypted Local Storage",
                                style = DmsTheme.typography.label.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = DmsColors.White92
                                )
                            )
                            Text(
                                text = "Credentials and OAuth refresh tokens are encrypted on-device via Android Keystore.",
                                style = DmsTheme.typography.caption.copy(
                                    fontSize = 10.sp,
                                    color = DmsColors.White48
                                )
                            )
                        }
                    }
                }
            }

            // Notification Toggle persisted to Room
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR14,
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = DmsColors.White80,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Real-time Push Alerts",
                                    style = DmsTheme.typography.label.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = DmsColors.White92
                                    )
                                )
                                Text(
                                    text = "Notify immediately on build failures and quota warnings",
                                    style = DmsTheme.typography.caption.copy(
                                        fontSize = 10.sp,
                                        color = DmsColors.White48
                                    )
                                )
                            }
                        }

                        DmsToggle(
                            checked = toggleStates.buildAlerts,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    notificationRepository.updateToggle { it.copy(buildAlerts = isChecked) }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom CTA Primary Button
        DmsPrimaryButton(
            text = "Continue to Command Center",
            onClick = onContinueClick,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun StepNode(
    step: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isActive) DmsColors.White else DmsColors.OledBlack)
            .border(
                BorderStroke(1.dp, if (isActive) DmsColors.White else DmsColors.White20),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = DmsColors.OledBlack,
                modifier = Modifier.size(12.dp)
            )
        } else {
            Text(
                text = step,
                style = DmsTheme.typography.caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isActive) DmsColors.OledBlack else DmsColors.White64
                )
            )
        }
    }
}

@Composable
private fun StepConnector(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(2.dp)
            .background(if (isActive) DmsColors.White else DmsColors.White14)
    )
}
