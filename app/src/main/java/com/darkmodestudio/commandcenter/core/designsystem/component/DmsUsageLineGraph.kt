package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors

@Composable
fun DmsUsageLineGraph(
    dataPoints: List<Float>, // Values e.g. 0.2f, 0.4f, 0.35f, 0.7f, 0.65f, 0.85f
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (dataPoints.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val paddingBottom = 4.dp.toPx()
        val paddingTop = 6.dp.toPx()
        val usableHeight = h - paddingTop - paddingBottom

        // 1. Draw horizontal dotted grid lines
        val gridLines = 3
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f), 0f)
        for (i in 0..gridLines) {
            val y = paddingTop + (usableHeight / gridLines) * i
            drawLine(
                color = DmsColors.White10,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // Calculate points
        val stepX = w / (dataPoints.size - 1)
        val points = dataPoints.mapIndexed { index, value ->
            val x = index * stepX
            val y = paddingTop + (1f - value.coerceIn(0f, 1f)) * usableHeight
            Offset(x, y)
        }

        // 2. Build Smooth Spline Path
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
        }

        // 3. Subtle 4-7% White Gradient Fill Beneath
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h - paddingBottom)
            lineTo(0f, h - paddingBottom)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    DmsColors.White.copy(alpha = 0.08f),
                    DmsColors.White.copy(alpha = 0.01f)
                ),
                startY = paddingTop,
                endY = h
            )
        )

        // 4. White 2dp Line
        drawPath(
            path = path,
            color = DmsColors.White,
            style = Stroke(width = 2.dp.toPx())
        )

        // 5. Final White Node at the end
        val lastPoint = points.last()
        // Outer halo
        drawCircle(
            color = DmsColors.White20,
            radius = 5.dp.toPx(),
            center = lastPoint
        )
        // Solid center
        drawCircle(
            color = DmsColors.White,
            radius = 3.dp.toPx(),
            center = lastPoint
        )
    }
}
