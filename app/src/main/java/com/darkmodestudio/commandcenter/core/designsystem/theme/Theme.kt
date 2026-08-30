package com.darkmodestudio.commandcenter.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = DmsColors.White,
    onPrimary = DmsColors.OledBlack,
    surface = DmsColors.Surface01,
    onSurface = DmsColors.White,
    background = DmsColors.OledBlack,
    onBackground = DmsColors.White,
    surfaceVariant = DmsColors.Surface02,
    onSurfaceVariant = DmsColors.White80,
    outline = DmsColors.CardBorder
)

val LocalDmsTypography = staticCompositionLocalOf { DmsTypography() }
val LocalDmsColors = staticCompositionLocalOf { DmsColors }

@Composable
fun DarkModeStudioTheme(
    content: @Composable () -> Unit
) {
    val typography = DmsTypography()

    CompositionLocalProvider(
        LocalDmsTypography provides typography,
        LocalDmsColors provides DmsColors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            content = content
        )
    }
}

object DmsTheme {
    val typography: DmsTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalDmsTypography.current

    val colors: DmsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDmsColors.current
}
