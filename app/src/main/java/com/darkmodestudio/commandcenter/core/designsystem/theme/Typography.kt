package com.darkmodestudio.commandcenter.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DmsFontFamily = FontFamily.SansSerif

@Immutable
data class DmsTypography(
    val displayXL: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
        color = DmsColors.White
    ),
    val displayL: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
        color = DmsColors.White
    ),
    val displayM: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.5).sp,
        color = DmsColors.White
    ),
    val h1: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        color = DmsColors.White
    ),
    val h2: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        color = DmsColors.White
    ),
    val h3: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        color = DmsColors.White
    ),
    val h4: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = DmsColors.White
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = DmsColors.White92
    ),
    val body: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = DmsColors.White80
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = DmsColors.White64
    ),
    val label: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = DmsColors.White80
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = DmsColors.White48
    ),
    val brandTitle: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp,
        color = DmsColors.White
    ),
    val brandSubtitle: TextStyle = TextStyle(
        fontFamily = DmsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 4.5.sp,
        color = DmsColors.White64
    )
)
