package com.darkmodestudio.commandcenter.feature.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.data.repository.SettingsRepository
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsCard
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsSecondaryOutlineButton
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsStatusCapsule
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsToggle
import com.darkmodestudio.commandcenter.core.designsystem.component.DmsTopBar
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit,
    onManageAutomationsClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by settingsRepository.userProfile.collectAsState(initial = com.darkmodestudio.commandcenter.core.model.UserProfile())
    val automationStats by settingsRepository.automationStats.collectAsState(initial = com.darkmodestudio.commandcenter.core.model.AutomationStats())
    val biometricLock by settingsRepository.biometricLock.collectAsState(initial = true)
    val dailyBriefing by settingsRepository.dailyBriefing.collectAsState(initial = true)
    val syncFrequency by settingsRepository.syncFrequency.collectAsState(initial = "15 minutes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DmsColors.OledBlack)
    ) {
        // App Top Bar with Back Arrow
        DmsTopBar(
            title = "Dark Mode Studio",
            subtitle = "settings",
            showBack = true,
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DmsSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // USER PROFILE CARD
            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR20,
                    backgroundColor = DmsColors.Surface01,
                    padding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DmsColors.Surface02)
                                    .border(BorderStroke(1.dp, DmsColors.White), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.initials,
                                    style = DmsTheme.typography.h3.copy(color = DmsColors.White)
                                )
                            }

                            Column {
                                Text(
                                    text = userProfile.name,
                                    style = DmsTheme.typography.h3.copy(fontSize = 16.sp)
                                )
                                Text(
                                    text = userProfile.email,
                                    style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileStat(label = "Projects", value = "${userProfile.projectCount}")
                            ProfileStat(label = "Automations", value = "${userProfile.automationsCount}")
                            ProfileStat(label = "Cached", value = userProfile.syncedDataSize)
                            ProfileStat(label = "Uptime", value = userProfile.uptime)
                        }
                    }
                }
            }

            // APP PREFERENCES
            item {
                SectionHeader(title = "App Preferences")
            }

            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR18,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PreferenceRow(
                            icon = Icons.Outlined.Palette,
                            title = "Theme",
                            value = "Pure Black & White (OLED)"
                        )
                        Divider()
                        PreferenceRow(
                            icon = Icons.Outlined.Language,
                            title = "Language",
                            value = "English (US)"
                        )
                    }
                }
            }

            // CONNECTED ACCOUNTS
            item {
                SectionHeader(title = "Connected Accounts")
            }

            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR18,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccountRow(name = "GitHub", account = "darkmodestudio")
                        Divider()
                        AccountRow(name = "Google Drive", account = "founder@dms.dev")
                        Divider()
                        AccountRow(name = "Slack", account = "#command-center")
                        Divider()
                        AccountRow(name = "Notion", account = "DMS Workspace")
                    }
                }
            }

            // GENERAL SETTINGS
            item {
                SectionHeader(title = "Security & Sync")
            }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = DmsColors.White80,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Biometric Lock",
                                        style = DmsTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DmsColors.White
                                        )
                                    )
                                    Text(
                                        text = "Require fingerprint / face scan on app open",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                }
                            }

                            DmsToggle(
                                checked = biometricLock,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        settingsRepository.toggleBiometricLock()
                                    }
                                }
                            )
                        }

                        Divider()

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
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    tint = DmsColors.White80,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Background Sync",
                                        style = DmsTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DmsColors.White
                                        )
                                    )
                                    Text(
                                        text = "Refresh rate: $syncFrequency",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                }
                            }

                            DmsStatusCapsule(text = "15m", height = 24.dp)
                        }

                        Divider()

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
                                        text = "Daily Briefing",
                                        style = DmsTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = DmsColors.White
                                        )
                                    )
                                    Text(
                                        text = "Morning executive summary at 08:30 AM",
                                        style = DmsTheme.typography.caption.copy(color = DmsColors.White48)
                                    )
                                }
                            }

                            DmsToggle(
                                checked = dailyBriefing,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        settingsRepository.toggleDailyBriefing()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // AUTOMATIONS
            item {
                SectionHeader(title = "Automations")
            }

            item {
                DmsCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DmsRadii.ShapeR18,
                    backgroundColor = DmsColors.Surface01,
                    padding = 14.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileStat(label = "Active Rules", value = "${automationStats.activeRules}")
                            ProfileStat(label = "Inactive", value = "${automationStats.inactiveRules}")
                            ProfileStat(label = "24h Executions", value = "${automationStats.executionsLast24h}")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        DmsSecondaryOutlineButton(
                            text = "Manage Automation Rules",
                            onClick = { onManageAutomationsClick?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                            height = 38.dp
                        )
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = DmsTheme.typography.h3.copy(fontSize = 16.sp),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
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
private fun PreferenceRow(icon: ImageVector, title: String, value: String) {
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
                imageVector = icon,
                contentDescription = null,
                tint = DmsColors.White80,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = DmsTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DmsColors.White
                )
            )
        }
        Text(
            text = value,
            style = DmsTheme.typography.caption.copy(color = DmsColors.White64)
        )
    }
}

@Composable
private fun AccountRow(name: String, account: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                style = DmsTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DmsColors.White
                )
            )
            Text(
                text = account,
                style = DmsTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    color = DmsColors.White48
                )
            )
        }

        DmsStatusCapsule(
            text = "Active",
            height = 22.dp,
            borderColor = DmsColors.White20
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DmsColors.White10)
    )
}
