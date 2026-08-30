package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors

enum class NodeStyle {
    SOLID,
    HOLLOW,
    DOUBLE_RING,
    DOTTED,
    SLASH,
    CHECK
}

@Composable
fun DmsNode(
    modifier: Modifier = Modifier,
    style: NodeStyle = NodeStyle.SOLID,
    size: Dp = 6.dp,
    color: Color = DmsColors.White
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)

        when (style) {
            NodeStyle.SOLID -> {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center
                )
            }
            NodeStyle.HOLLOW -> {
                drawCircle(
                    color = color,
                    radius = radius - 1f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            NodeStyle.DOUBLE_RING -> {
                drawCircle(
                    color = color,
                    radius = radius - 1f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = color,
                    radius = radius * 0.45f,
                    center = center
                )
            }
            NodeStyle.DOTTED -> {
                drawCircle(
                    color = color,
                    radius = radius - 1f,
                    center = center,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                    )
                )
            }
            NodeStyle.SLASH -> {
                drawCircle(
                    color = color,
                    radius = radius - 1f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    color = color,
                    start = Offset(radius * 0.35f, radius * 1.65f),
                    end = Offset(radius * 1.65f, radius * 0.35f),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            NodeStyle.CHECK -> {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center
                )
                // Draw inner checkmark in black
                val checkColor = DmsColors.OledBlack
                val strokeW = (radius * 0.35f).coerceAtLeast(1.5f)
                drawLine(
                    color = checkColor,
                    start = Offset(radius * 0.6f, radius * 1.0f),
                    end = Offset(radius * 0.9f, radius * 1.35f),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = checkColor,
                    start = Offset(radius * 0.85f, radius * 1.35f),
                    end = Offset(radius * 1.45f, radius * 0.65f),
                    strokeWidth = strokeW
                )
            }
        }
    }
}
