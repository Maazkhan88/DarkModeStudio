package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsTheme

@Composable
fun DmsStatusCapsule(
    text: String,
    modifier: Modifier = Modifier,
    nodeStyle: NodeStyle = NodeStyle.SOLID,
    nodeColor: Color = DmsColors.White,
    height: Dp = 28.dp,
    backgroundColor: Color = DmsColors.Surface01,
    borderColor: Color = DmsColors.White20
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(DmsRadii.ShapeCapsule)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), DmsRadii.ShapeCapsule)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DmsNode(
            style = nodeStyle,
            size = 4.dp,
            color = nodeColor
        )
        Text(
            text = text,
            style = DmsTheme.typography.label.copy(
                fontSize = 11.sp,
                color = DmsColors.White92
            )
        )
    }
}

@Composable
fun DmsFilterCapsule(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val borderColor = if (isSelected) DmsColors.White else DmsColors.White14
    val backgroundColor = if (isSelected) DmsColors.White06 else Color.Transparent
    val textColor = if (isSelected) DmsColors.White else DmsColors.White48

    Box(
        modifier = modifier
            .height(30.dp)
            .clip(DmsRadii.ShapeCapsule)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), DmsRadii.ShapeCapsule)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                style = DmsTheme.typography.label.copy(
                    fontSize = 11.sp,
                    color = textColor
                )
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = DmsTheme.typography.caption.copy(
                        color = if (isSelected) DmsColors.White64 else DmsColors.White32
                    )
                )
            }
        }
    }
}
