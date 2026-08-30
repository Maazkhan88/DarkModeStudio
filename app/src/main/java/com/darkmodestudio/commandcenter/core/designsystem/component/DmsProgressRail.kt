package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii

@Composable
fun DmsProgressRail(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    trackColor: Color = DmsColors.White14,
    fillColor: Color = DmsColors.White
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "ProgressRailAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(DmsRadii.ShapeCapsule)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(DmsRadii.ShapeCapsule)
                .background(fillColor)
        )
    }
}
