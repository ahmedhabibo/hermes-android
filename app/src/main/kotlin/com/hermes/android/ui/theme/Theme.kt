package com.hermes.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom Hermes brand colors (fallback when dynamic color is unavailable)
private val HermesBlue = Color(0xFF3B6EF5)
private val HermesBlueDark = Color(0xFFA9C7FF)
private val HermesTeal = Color(0xFF00897B)
private val HermesTealDark = Color(0xFF80CBC4)

private val LightColors = lightColorScheme(
    primary = HermesBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = HermesTeal,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF003744),
    tertiary = Color(0xFF7D5260),
    error = Color(0xFFB3261E),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0E9),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
)

private val DarkColors = darkColorScheme(
    primary = HermesBlueDark,
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF1A468F),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = HermesTealDark,
    onSecondary = Color(0xFF003744),
    secondaryContainer = Color(0xFF004F5C),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFEFB8C8),
    error = Color(0xFFF2B8B8),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
)

@Composable
fun HermesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
