package com.hungerg.cookflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFFC4472D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE2D8),
    onPrimaryContainer = Color(0xFF5F1608),
    secondary = Color(0xFF2F6B58),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3EEE2),
    onSecondaryContainer = Color(0xFF103D2E),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF28221E),
    surface = Color.White,
    onSurface = Color(0xFF28221E),
    surfaceVariant = Color(0xFFF1EDE7),
    onSurfaceVariant = Color(0xFF6F665D),
    outline = Color(0xFFD9D0C6),
    outlineVariant = Color(0xFFE8E1D8),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9B7E),
    onPrimary = Color(0xFF5F1608),
    primaryContainer = Color(0xFF7E2F1D),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF9BD2BA),
    onSecondary = Color(0xFF103D2E),
    secondaryContainer = Color(0xFF285441),
    onSecondaryContainer = Color(0xFFD3EEE2),
    background = Color(0xFF1C1815),
    onBackground = Color(0xFFEDE2D8),
    surface = Color(0xFF25201C),
    onSurface = Color(0xFFEDE2D8),
    surfaceVariant = Color(0xFF332C26),
    onSurfaceVariant = Color(0xFFD1C4B8),
    outline = Color(0xFF9C8E82),
    outlineVariant = Color(0xFF4D4138),
    error = Color(0xFFFFB4AB)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 27.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 23.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CookFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
