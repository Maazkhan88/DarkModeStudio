package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

@Composable
fun DmsTopBar(
    modifier: Modifier = Modifier,
    title: String = "Dark Mode Studio",
    subtitle: String = "command center",
    showBack: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    hasNotifications: Boolean = true,
    customAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = DmsSpacing.ScreenHorizontal,
                vertical = DmsSpacing.Dp12
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (showBack && onBackClick != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DmsColors.Surface01)
                        .border(BorderStroke(1.dp, DmsColors.White14), CircleShape)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DmsColors.White80,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = title,
                    style = DmsTheme.typography.brandTitle
                )
                Text(
                    text = subtitle.lowercase(),
                    style = DmsTheme.typography.brandSubtitle
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customAction != null) {
                customAction()
            } else {
                if (onNotificationClick != null) {
                    DmsIconButton(
                        icon = Icons.Outlined.Notifications,
                        onClick = onNotificationClick,
                        hasNotification = hasNotifications,
                        contentDescription = "Notifications"
                    )
                }

                if (onAvatarClick != null) {
                    // 40dp Avatar with 1dp white outline
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DmsColors.Surface02)
                            .border(BorderStroke(1.dp, DmsColors.White), CircleShape)
                            .clickable(onClick = onAvatarClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AG",
                            style = DmsTheme.typography.label.copy(
                                color = DmsColors.White,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
