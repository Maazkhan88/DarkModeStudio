package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DmsOrbitalCanvas(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.52f, h * 0.48f)

        // 1. Center subtle dot matrix field (3x3 tiny dots)
        val gridStep = 6.dp.toPx()
        for (i in -1..1) {
            for (j in -1..1) {
                drawCircle(
                    color = DmsColors.White20,
                    radius = 1.2.dp.toPx(),
                    center = Offset(center.x + i * gridStep, center.y + j * gridStep)
                )
            }
        }

        // 2. Three concentric dotted rings in subtle White20
        val radii = floatArrayOf(w * 0.22f, w * 0.36f, w * 0.47f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)

        radii.forEachIndexed { index, r ->
            drawCircle(
                color = if (index == 1) DmsColors.White32 else DmsColors.White20,
                radius = r,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            )
        }

        // 3. Trajectory line: smooth curved elliptical arc crossing rings
        val trajectoryPath = Path().apply {
            moveTo(w * 0.08f, h * 0.88f)
            cubicTo(
                w * 0.35f, h * 0.85f,
                w * 0.75f, h * 0.45f,
                w * 0.92f, h * 0.12f
            )
        }
        drawPath(
            path = trajectoryPath,
            color = DmsColors.White64,
            style = Stroke(
                width = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )
        )

        // 4. Large primary active node (on outer ring / trajectory)
        val angle1 = Math.toRadians(42.0)
        val node1Pos = Offset(
            center.x + (radii[1] * cos(angle1)).toFloat(),
            center.y - (radii[1] * sin(angle1)).toFloat()
        )
        // Outer glow/ring for large node
        drawCircle(
            color = DmsColors.White32,
            radius = 6.5.dp.toPx(),
            center = node1Pos,
            style = Stroke(width = 1.dp.toPx())
        )
        // Solid center for large node
        drawCircle(
            color = DmsColors.White,
            radius = 3.5.dp.toPx(),
            center = node1Pos
        )

        // 5. Two smaller satellite nodes
        val angle2 = Math.toRadians(165.0)
        val node2Pos = Offset(
            center.x + (radii[2] * cos(angle2)).toFloat(),
            center.y - (radii[2] * sin(angle2)).toFloat()
        )
        drawCircle(
            color = DmsColors.White80,
            radius = 2.2.dp.toPx(),
            center = node2Pos
        )

        val angle3 = Math.toRadians(285.0)
        val node3Pos = Offset(
            center.x + (radii[0] * cos(angle3)).toFloat(),
            center.y - (radii[0] * sin(angle3)).toFloat()
        )
        drawCircle(
            color = DmsColors.White48,
            radius = 1.8.dp.toPx(),
            center = node3Pos
        )
    }
}
