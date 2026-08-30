package com.darkmodestudio.commandcenter.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsColors
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsRadii
import com.darkmodestudio.commandcenter.core.designsystem.theme.DmsSpacing

@Composable
fun DmsCard(
    modifier: Modifier = Modifier,
    shape: Shape = DmsRadii.ShapeR20,
    backgroundColor: Color = DmsColors.Surface01,
    borderColor: Color = DmsColors.CardBorder,
    borderWidth: Dp = 1.dp,
    padding: Dp = DmsSpacing.NormalCardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .then(clickableModifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun DmsHeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = DmsRadii.ShapeR22,
    backgroundColor: Color = DmsColors.Surface01,
    borderColor: Color = DmsColors.CardBorder,
    padding: Dp = DmsSpacing.HeroCardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    DmsCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        padding = padding,
        onClick = onClick,
        content = content
    )
}

@Composable
fun DmsCompactCard(
    modifier: Modifier = Modifier,
    shape: Shape = DmsRadii.ShapeR14,
    backgroundColor: Color = DmsColors.Surface01,
    borderColor: Color = DmsColors.CardBorder,
    padding: Dp = DmsSpacing.SmallCardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    DmsCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        padding = padding,
        onClick = onClick,
        content = content
    )
}
