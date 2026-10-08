package com.example.onemusic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Màu theo theme (Sáng / Tối), xem docs/COLOR_REDESIGN_PLAN.md. Dùng qua [AppTheme.colors] trong composable;
 * các hằng số cũ trong Color.kt (TextPrimary, SurfaceElevated…) chỉ còn là nguồn giá trị của [LegacyDarkAppColors]
 * (bảng riêng của Now Playing) và của vài chỗ ngoài ngữ cảnh composable.
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
    val onAccent: Color,          // chữ/icon in lên nền accent hoặc danger (luôn trắng)
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val danger: Color,            // xoá / yêu thích / cảnh báo nguy hiểm
    val warning: Color,
    val shadow: Color,
    val scrim: Color
) {
    // Thang màu chữ trong suốt: dùng cho viền, nền nút mờ, icon phụ trên bề mặt (thay cho Ivory*).
    val hairline: Color get() = textPrimary.copy(alpha = 0.06f)
    val subtle: Color get() = textPrimary.copy(alpha = 0.10f)
    val stroke: Color get() = textPrimary.copy(alpha = 0.16f)
    val muted: Color get() = textPrimary.copy(alpha = 0.24f)
    val disabled: Color get() = textPrimary.copy(alpha = 0.35f)
    val faint: Color get() = textPrimary.copy(alpha = 0.45f)
    val medium: Color get() = textPrimary.copy(alpha = 0.60f)
    val high: Color get() = textPrimary.copy(alpha = 0.85f)
}

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
    onAccent = Color(0xFFFFFFFF),
    accent = Color(0xFF0A84FF),            // chữ, icon, thanh trượt trên nền tối
    accentLight = Color(0xFF4DA3FF),
    accentDark = Color(0xFF0066D6),        // nền nút đặc (chữ trắng đạt 5.42)
    danger = ApexRose,
    warning = ApexAmber,
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
    onAccent = PrimaryIvory,
    accent = Brand,
    accentLight = BrandLight,
    accentDark = BrandDark,
    danger = ApexRose,
    warning = ApexAmber,
    shadow = ShadowColor,
    scrim = ScrimColor
)

// Bảng màu sáng (nền trắng, chữ đen, nhấn xanh dương), xem docs/COLOR_REDESIGN_PLAN.md mục 2.
val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surfaceBase = Color(0xFFF8F8FA),
    surface1 = Color(0xFFF2F2F7),          // dock, mini player, dialog, sheet, popup
    surfaceControl = Color(0xFFE5E5EA),
    surface2 = Color(0xFFE5E5EA),          // thẻ card
    surfaceActive = Color(0xFFDCDCE0),
    surfaceActiveIndicator = Color(0xFFD1D1D6),
    activePill = Color(0xFFC7C7CC),
    divider = Color(0xFFD1D1D6),
    borderStrong = Color(0xFFC6C6C8),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF3C3C43),
    textTertiary = Color(0xFF737377),      // 4.7 trên nền trắng
    textDisabled = Color(0xFFAEAEB2),
    onInverse = Color(0xFFFFFFFF),
    onAccent = Color(0xFFFFFFFF),
    accent = Color(0xFF0066D6),            // 5.42 trên trắng, 4.86 trên #F2F2F7
    accentLight = Color(0xFF0A84FF),
    accentDark = Color(0xFF004FA8),
    danger = Color(0xFFD70015),            // 5.38 trên trắng
    warning = Color(0xFFB26A00),
    shadow = Color(0x2E000000),            // đen 18%: bóng nhẹ trên nền sáng
    scrim = ScrimColor
)

/** Lớp bọc cho vùng nằm trên ảnh/gradient tối (banner, hero): luôn dùng chữ trắng bất kể theme. */
@Composable
fun OnImageScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides DarkAppColors, content = content)
}

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

/**
 * Số vùng Now Playing đang mở (sheet, hộp thoại mở từ trong sheet). Khi > 0, [OneMusicTheme] giữ biểu tượng
 * thanh trạng thái/điều hướng ở dạng sáng (đọc được trên nền tối) dù theme của app là sáng.
 * Đọc trong composition nên [OneMusicTheme] tự áp lại khi giá trị đổi hoặc khi theme hệ thống đổi.
 */
internal object ThemeOverrides {
    var nowPlayingOpenCount by mutableIntStateOf(0)
}

/**
 * Bọc Now Playing: luôn dùng bảng màu tối CŨ ([LegacyDarkAppColors]) ở mọi theme (màn này có nền động theo
 * ảnh bìa và được giữ nguyên), và báo cho [OneMusicTheme] giữ biểu tượng thanh hệ thống ở dạng sáng.
 */
@Composable
fun NowPlayingThemeScope(content: @Composable () -> Unit) {
    DisposableEffect(Unit) {
        ThemeOverrides.nowPlayingOpenCount++
        onDispose { ThemeOverrides.nowPlayingOpenCount-- }
    }
    CompositionLocalProvider(LocalAppColors provides LegacyDarkAppColors) {
        MaterialTheme(
            colorScheme = LegacyMaterialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
