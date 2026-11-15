package com.clblox.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** CLblox palette: a dusk-violet night with four chunky "block" colors. */
object CL {
    val Night = Color(0xFF1B1530)
    val NightRaised = Color(0xFF261E42)
    val Violet = Color(0xFF6C4DFF)
    val Coral = Color(0xFFFF6B5E)
    val Butter = Color(0xFFFFD66B)
    val Mint = Color(0xFF5EE6C3)
    val Mist = Color(0xFFEDE9FF)
    val MistDim = Color(0xFFA79FCB)

    private val tiles = listOf(Violet, Coral, Butter, Mint)

    /** Every game gets a stable color from its place id. */
    fun tileColor(seed: Long): Color = tiles[(((seed % 4) + 4) % 4).toInt()]

    /** Readable ink color for text on a block. */
    fun onTile(color: Color): Color = if (color == Violet) Color.White else Night
}

private val Sans = FontFamily.SansSerif

private val CLTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Black,
        fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Black,
        fontSize = 26.sp, lineHeight = 28.sp, letterSpacing = (-0.8).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = (-0.2).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 18.sp
    )
)

@Composable
fun CLTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CL.Violet,
            onPrimary = Color.White,
            secondary = CL.Butter,
            background = CL.Night,
            onBackground = CL.Mist,
            surface = CL.NightRaised,
            onSurface = CL.Mist,
            error = CL.Coral
        ),
        typography = CLTypography,
        content = content
    )
}
