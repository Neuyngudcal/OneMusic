package com.example.onemusic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Màu theo theme (Sáng / Tối). Bước 1 của docs/COLOR_REDESIGN_PLAN.md: chỉ dựng hạ tầng,
 * [LightAppColors] tạm thời GIỐNG [DarkAppColors] (bảng tối mới, bước 3).
 * Các hằng số cũ trong Color.kt (TextPrimary, SurfaceElevated…) vẫn là nguồn giá trị;
 * những chỗ dùng sẽ chuyển sang [AppTheme.colors] ở bước 2.
 */
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surfaceBase: Color,
    val surface1: Color,          // dock, mini player, dialog, sheet, popup
    val surfaceControl: Color,
    val surface2: Color,          // thẻ card
    val surfaceActive: Color,
    val surfaceActiveIndicator: Color,
    val activePill: Color,
    val divider: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val onInverse: Color,         // chữ/icon in lên nền màu chữ chính, ví dụ pill đang chọn (CharcoalBlack)
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val shadow: Color,
    val scrim: Color
)

// Bảng màu tối mới (trắng – xanh dương – đen), xem docs/COLOR_REDESIGN_PLAN.md mục 2.
val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF000000),
    surfaceBase = Color(0xFF0A0A0B),
    surface1 = Color(0xFF1C1C1E),          // dock, mini player, dialog, sheet, popup
    surfaceControl = Color(0xFF2C2C2E),
    surface2 = Color(0xFF2C2C2E),          // thẻ card
    surfaceActive = Color(0xFF323234),
    surfaceActiveIndicator = Color(0xFF3A3A3C),
    activePill = Color(0xFF48484A),
    divider = Color(0xFF38383A),
    borderStrong = Color(0xFF48484A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1A6),
    textTertiary = Color(0xFF8E8E93),
    textDisabled = Color(0xFF636366),
    onInverse = Color(0xFF000000),
    accent = Color(0xFF0A84FF),            // chữ, icon, thanh trượt trên nền tối
    accentLight = Color(0xFF4DA3FF),
    accentDark = Color(0xFF0066D6),        // nền nút đặc (chữ trắng đạt 5.42)
    shadow = ShadowColor,
    scrim = ScrimColor
)

/**
 * Bảng màu cũ (ngà ấm, xanh lá) — GIỮ NGUYÊN cho Now Playing theo quyết định "giữ nguyên".
 * Giá trị lấy từ các hằng số cũ trong Color.kt, không đổi.
 */
val LegacyDarkAppColors = AppColors(
    isDark = true,
    background = ObsidianBlack,
    surfaceBase = SurfaceBase,
    surface1 = SurfaceElevated,
    surfaceControl = SurfaceControl,
    surface2 = SurfaceCard,
    surfaceActive = SurfaceActive,
    surfaceActiveIndicator = SurfaceActiveIndicator,
    activePill = ActivePillBg,
    divider = SurfaceDivider,
    borderStrong = SurfaceBorderStrong,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    textDisabled = TextDisabled,
    onInverse = CharcoalBlack,
    accent = Brand,
    accentLight = BrandLight,
    accentDark = BrandDark,
    shadow = ShadowColor,
    scrim = ScrimColor
)

// TODO(bước 4): thay bằng bảng màu sáng thật. Tạm thời giống tối, nên isDark vẫn là true
// để biểu tượng thanh hệ thống đọc được trên nền đen.
val LightAppColors = DarkAppColors

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

/**
 * Bọc Now Playing: luôn dùng bảng màu tối CŨ ([LegacyDarkAppColors]) ở mọi theme (màn này có nền động theo ảnh bìa),
 * và giữ biểu tượng thanh trạng thái/điều hướng ở dạng sáng trong lúc sheet mở.
 */
@Composable
fun NowPlayingThemeScope(content: @Composable () -> Unit) {
    val outer = LocalAppColors.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(outer) {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
            onDispose {
                if (window != null) {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !outer.isDark
                    controller.isAppearanceLightNavigationBars = !outer.isDark
                }
            }
        }
    }
    CompositionLocalProvider(LocalAppColors provides LegacyDarkAppColors) {
        MaterialTheme(
            colorScheme = LegacyMaterialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
