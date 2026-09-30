package com.dolphin.quicklens.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80D4FF),
    onPrimary = Color(0xFF003548),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = Color(0xFFB5CAD7),
    onSecondary = Color(0xFF20333D),
    surface = Color(0xFF111416),
    onSurface = Color(0xFFE1E3E5),
    surfaceVariant = Color(0xFF1E2328),
    onSurfaceVariant = Color(0xFFC0C7CD)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006689),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FF),
    onPrimaryContainer = Color(0xFF001E2B),
    secondary = Color(0xFF4D616C),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFBFCFE),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDEE3EA),
    onSurfaceVariant = Color(0xFF41484D)
)

@Composable
fun QuickLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
