package com.example.onemusic.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.onemusic.data.local.ThemeMode

// Mọi component Material (TextField, Switch, ripple, Snackbar…) lấy màu từ scheme dựng theo AppColors.
internal fun materialColorSchemeFor(c: AppColors) =
    if (c.isDark) darkColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentDark,
        onPrimaryContainer = c.onAccent,
        inversePrimary = c.accentLight,
        secondary = c.textPrimary,
        onSecondary = c.onInverse,
        secondaryContainer = c.surfaceActiveIndicator,
        onSecondaryContainer = c.textPrimary,
        tertiary = ApexCyan,
        onTertiary = c.onInverse,
        background = c.background,
        onBackground = c.textPrimary,
        surface = c.surface1,
        onSurface = c.textPrimary,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.textSecondary,
        surfaceTint = Color.Transparent,
        surfaceBright = c.surfaceActive,
        surfaceDim = c.background,
        surfaceContainerLowest = c.background,
        surfaceContainerLow = c.surfaceBase,
        surfaceContainer = c.surface1,
        surfaceContainerHigh = c.surface2,
        surfaceContainerHighest = c.surfaceActive,
        inverseSurface = c.textPrimary,
        inverseOnSurface = c.onInverse,
        error = c.danger,
        onError = c.onAccent,
        outline = c.borderStrong,
        outlineVariant = c.divider,
        scrim = c.scrim
    ) else lightColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentDark,
        onPrimaryContainer = c.onAccent,
        inversePrimary = c.accentLight,
        secondary = c.textPrimary,
        onSecondary = c.onInverse,
        secondaryContainer = c.surfaceActiveIndicator,
        onSecondaryContainer = c.textPrimary,
        tertiary = ApexCyan,
        onTertiary = c.onInverse,
        background = c.background,
        onBackground = c.textPrimary,
        surface = c.surface1,
        onSurface = c.textPrimary,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.textSecondary,
        surfaceTint = Color.Transparent,
        surfaceBright = c.surfaceActive,
        surfaceDim = c.background,
        surfaceContainerLowest = c.background,
        surfaceContainerLow = c.surfaceBase,
        surfaceContainer = c.surface1,
        surfaceContainerHigh = c.surface2,
        surfaceContainerHighest = c.surfaceActive,
        inverseSurface = c.textPrimary,
        inverseOnSurface = c.onInverse,
        error = c.danger,
        onError = c.onAccent,
        outline = c.borderStrong,
        outlineVariant = c.divider,
        scrim = c.scrim
    )

internal val DarkMaterialColorScheme = materialColorSchemeFor(DarkAppColors)
/** Scheme bản cũ (ngà ấm), dùng riêng cho Now Playing. */
internal val LegacyMaterialColorScheme = materialColorSchemeFor(LegacyDarkAppColors)

private val LightMaterialColorScheme = materialColorSchemeFor(LightAppColors)

@Composable
fun OneMusicTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val typography = remember(appColors) { typographyFor(appColors) }
    // Now Playing mở: giữ biểu tượng thanh hệ thống dạng sáng (nền tối), bất kể theme
    val lightSystemBarIcons = !appColors.isDark && ThemeOverrides.nowPlayingOpenCount == 0
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkMaterialColorScheme
        else -> LightMaterialColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightSystemBarIcons
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = lightSystemBarIcons
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
