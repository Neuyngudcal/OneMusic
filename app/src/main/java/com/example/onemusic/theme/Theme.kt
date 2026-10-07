package com.example.onemusic.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Scheme duy nhất của app: AMOLED đen + bề mặt ngà ấm + chữ trắng ngà, màu nhấn Forest Green.
// Mọi component Material (TextField, Switch, ripple, Snackbar…) lấy màu từ đây.
private val DarkColorScheme = darkColorScheme(
    primary = Brand,
    onPrimary = PrimaryIvory,
    primaryContainer = BrandDark,
    onPrimaryContainer = PrimaryIvory,
    inversePrimary = BrandLight,
    secondary = PrimaryIvory,
    onSecondary = CharcoalBlack,
    secondaryContainer = SurfaceActiveIndicator,
    onSecondaryContainer = TextPrimary,
    tertiary = ApexCyan,
    onTertiary = CharcoalBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceBright = SurfaceActive,
    surfaceDim = ObsidianBlack,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = SurfaceBase,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceActive,
    inverseSurface = PrimaryIvory,
    inverseOnSurface = CharcoalBlack,
    error = ApexRose,
    onError = PrimaryIvory,
    outline = SurfaceBorderStrong,
    outlineVariant = SurfaceDivider,
    scrim = ScrimColor
)

@Composable
fun OneMusicTheme(
    darkTheme: Boolean = true, // Default to Obsidian Space for true high-contrast audio experience
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
