package com.example.call.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.call.data.model.AppSettings
import com.example.call.data.model.ThemeMode

/** Extra colours that Material 3 has no slot for. */
data class CallColors(
    val call: Color,
    val decline: Color,
    val accent: Color,
    val card: Color,
    val isDark: Boolean
)

private val LocalCallColors = staticCompositionLocalOf {
    CallColors(CallGreen, DeclineRed, LightPurple, CardWhite, false)
}

object CallTheme {
    val colors: CallColors
        @Composable @ReadOnlyComposable get() = LocalCallColors.current
}

private val LightScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = Lavender,
    onPrimaryContainer = DeepPurple,
    secondary = Lavender,
    onSecondary = DeepPurple,
    secondaryContainer = LightPurple,
    onSecondaryContainer = DeepPurple,
    background = SoftCream,
    onBackground = Color(0xFF2B1A44),
    surface = CardWhite,
    onSurface = Color(0xFF2B1A44),
    surfaceVariant = LightPurple,
    onSurfaceVariant = Color(0xFF5B4A78),
    outline = Lavender,
    error = DeclineRed
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkAccent,
    onPrimaryContainer = Color.White,
    secondary = DarkAccent,
    onSecondary = Color.White,
    secondaryContainer = DarkAccent,
    onSecondaryContainer = Color.White,
    background = DarkBackground,
    onBackground = Color(0xFFF4EBFF),
    surface = DarkCard,
    onSurface = Color(0xFFF4EBFF),
    surfaceVariant = DarkAccent,
    onSurfaceVariant = Color(0xFFD9C8F2),
    outline = Color(0xFF5A4577),
    error = DeclineRed
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
)

@Composable
fun CallAppTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) {
        CallColors(CallGreen, DeclineRed, DarkAccent, DarkCard, true)
    } else {
        CallColors(CallGreen, DeclineRed, LightPurple, CardWhite, false)
    }
    val base = LocalDensity.current
    // Text size setting scales only text, never the layout density.
    val scaled = Density(base.density, base.fontScale * settings.textScale.factor)

    CompositionLocalProvider(
        LocalCallColors provides colors,
        LocalDensity provides scaled
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
