package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors

@Composable
fun DmsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) DmsColors.White else DmsColors.ToggleTrackOff,
        animationSpec = tween(durationMillis = 180),
        label = "ToggleTrackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) DmsColors.OledBlack else DmsColors.ToggleThumbOff,
        animationSpec = tween(durationMillis = 180),
        label = "ToggleThumbColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = tween(durationMillis = 180),
        label = "ToggleThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(18.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
