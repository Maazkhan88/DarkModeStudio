package com.darkmodestudio.commandcenter.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object DmsColors {
    // Pure OLED Canvas and Elevated Monochrome Surfaces
    val OledBlack = Color(0xFF000000)
    val Surface01 = Color(0xFF050505)
    val Surface02 = Color(0xFF090909)
    val Surface03 = Color(0xFF0D0D0D)
    val SurfaceSelected = Color(0xFF171717)
    val SurfaceNavSelected = Color(0xFF1C1C1C)

    // Controlled Grayscale / Typography Tokens
    val White = Color(0xFFFFFFFF)
    val White92 = Color(0xFFEBEBEB)
    val White80 = Color(0xFFCCCCCC)
    val White64 = Color(0xFFA3A3A3)
    val White48 = Color(0xFF7A7A7A)
    val White32 = Color(0xFF525252)
    val White20 = Color(0xFF333333)
    val White14 = Color(0xFF242424)
    val White10 = Color(0xFF1A1A1A)
    val White06 = Color(0xFF0F0F0F)

    // Subtle Borders
    val CardBorder = Color(0xFFFFFFFF).copy(alpha = 0.14f)
    val CardBorderHover = Color(0xFFFFFFFF).copy(alpha = 0.22f)
    val ActiveBorder = Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val Divider = Color(0xFF242424) // White14

    // Toggle Specific Tokens
    val ToggleTrackOff = Color(0xFF262626)
    val ToggleThumbOff = Color(0xFF888888)
}
