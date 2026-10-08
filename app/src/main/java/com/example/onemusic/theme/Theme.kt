package com.example.onemusic.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.onemusic.data.local.ThemeMode

// Scheme duy nhất của app: AMOLED đen + bề mặt ngà ấm + chữ trắng ngà, màu nhấn Forest Green.
// Mọi component Material (TextField, Switch, ripple, Snackbar…) lấy màu từ đây.
internal val DarkMaterialColorScheme = darkColorScheme(
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

// TODO(bước 4): thay bằng lightColorScheme thật. Tạm thời giống tối.
private val LightMaterialColorScheme = DarkMaterialColorScheme

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
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !appColors.isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !appColors.isDark
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
