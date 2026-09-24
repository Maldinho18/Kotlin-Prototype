package com.sidequests.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ExplorerIndigo = Color(0xFF5B4BDB)
val ExplorerIndigoLight = Color(0xFF7B6DE8)
val QuestAmber = Color(0xFFFFB347)
val DiscoveryTeal = Color(0xFF2AB7A9)
val DiscoveryTealDark = Color(0xFF1E9E92)

val LightBackground = Color(0xFFF6F5FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFEEEAFF)
val LightText = Color(0xFF1A1830)
val LightBorder = Color(0xFFDDD9FF)

val DarkBackground = Color(0xFF0E0D1A)
val DarkSurface = Color(0xFF19182D)
val DarkSurfaceAlt = Color(0xFF232140)
val DarkText = Color(0xFFEDE9FF)
val DarkBorder = Color(0xFF2C2A4A)

private val LightColors = lightColorScheme(
    primary = ExplorerIndigo,
    secondary = DiscoveryTeal,
    tertiary = QuestAmber,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceAlt,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    outline = LightBorder,
)

private val DarkColors = darkColorScheme(
    primary = ExplorerIndigoLight,
    secondary = DiscoveryTeal,
    tertiary = QuestAmber,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceAlt,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText,
    outline = DarkBorder,
)

val SidequestsTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

@Composable
fun SidequestsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SidequestsTypography,
        content = content,
    )
}
