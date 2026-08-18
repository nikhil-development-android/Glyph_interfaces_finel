package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlyphDarkColorScheme = darkColorScheme(
    primary = GlyphWhite,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceHighlight,
    onPrimaryContainer = GlyphWhite,
    secondary = NothingRed,
    onSecondary = GlyphWhite,
    secondaryContainer = NothingRedDark,
    onSecondaryContainer = GlyphWhite,
    tertiary = GlyphWhiteOff,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    outlineVariant = GlyphOffBorder
)

private val GlyphLightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceHighlight,
    onPrimaryContainer = Color.Black,
    secondary = NothingRedLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCDD),
    onSecondaryContainer = Color(0xFF68000A),
    tertiary = Color(0xFF333333),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = LightBorderDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GlyphDarkColorScheme else GlyphLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

