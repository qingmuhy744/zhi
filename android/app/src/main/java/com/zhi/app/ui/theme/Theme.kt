package com.zhi.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zhi.app.data.preferences.ThemeMode

// WildString 荒原浅色体系（米白荒原）
private val LightColorScheme = lightColorScheme(
    primary = WildOrange,
    onPrimary = Color.White,
    primaryContainer = WildOrangeContainerLight,
    onPrimaryContainer = Color(0xFF7A1E08),
    secondary = WildOrange,
    onSecondary = Color.White,
    background = WildRiceWhite,
    surface = WildSurfaceLight,
    surfaceContainer = WildContainerLight,
    surfaceVariant = WildVariantLight,
    onSurface = WildTextPrimaryLight,
    onSurfaceVariant = WildTextSecondaryLight,
    outline = WildBorderLight,
    outlineVariant = Color(0xFFCCC7BC)
)

// WildString 荒原深色体系（曜石黑夜）
private val DarkColorScheme = darkColorScheme(
    primary = WildOrangeLight,
    onPrimary = WildObsidian,
    primaryContainer = WildOrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = WildOrangeLight,
    onSecondary = WildObsidian,
    background = WildObsidian,
    surface = WildSurfaceDark,
    surfaceContainer = WildContainerDark,
    surfaceVariant = WildVariantDark,
    onSurface = WildTextPrimaryDark,
    onSurfaceVariant = WildTextSecondaryDark,
    outline = WildBorderDark,
    outlineVariant = Color(0xFF4A4B43)
)

@Composable
fun ZhiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
