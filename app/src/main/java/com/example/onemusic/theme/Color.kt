package com.example.onemusic.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// ============================================================================
// OneMusic Warm Ivory Palette — nguồn màu DUY NHẤT của ứng dụng.
// Mọi bề mặt dùng chung một sắc độ ngà ấm (hue ~40°, bão hoà thấp); chữ dùng
// trắng ngà #F4F1EA. Không viết cứng Color(0x...) / Color.White ngoài thư mục theme.
// Bản XML tương ứng (widget, splash): res/values/colors.xml — sửa ở đây thì sửa cả ở đó.
// ============================================================================

// ---- Surfaces (tối → sáng) -------------------------------------------------
val ObsidianBlack = Color(0xFF000000)                // Level 0: Nền AMOLED đen tuyền
val SurfaceBase = Color(0xFF0E0D0C)                  // Level 0.5: Canvas phụ
val SurfaceElevated = Color(0xFF24221F)              // Level 1: Dock, mini player, dialog, sheet, popup
val SurfaceControl = Color(0xFF262422)               // Nút / ô nhập đặt trên SurfaceElevated
val SurfaceCard = Color(0xFF2A2825)                  // Level 2: Thẻ card, nhóm danh sách
val SurfaceActive = Color(0xFF2D2A26)                // Trạng thái nhấn / item đang chọn
val SurfaceActiveIndicator = Color(0xFF35322D)       // Pill chỉ báo tab / chip đang chọn
val ActivePillBg = Color(0xFF423E38)                 // Pill bật (Shuffle / Repeat / Autoplay)

// ---- Viền & vạch kẻ -----------------------------------------------------------
val SurfaceDivider = Color(0xFF2C2A26)               // Vạch kẻ, viền mảnh của chip / search bar
val SurfaceBorderStrong = Color(0xFF3D3A35)          // Viền nút pill trong dialog, chip chưa chọn

// ---- Ivory (chữ & nội dung) ----------------------------------------------------
val PrimaryIvory = Color(0xFFF4F1EA)                 // Trắng ngà chính
val IvoryBody = Color(0xFFDDD9D0)                    // Văn bản dài / tên ca sĩ
val MutedIvory = Color(0xFF9C9890)                   // Chữ mờ, metadata
val CharcoalBlack = Color(0xFF121212)                // Chữ / icon in lên nền trắng ngà hoặc vàng

val TextPrimary = PrimaryIvory                       // Tiêu đề, chữ chính, icon chính
val TextSecondary = IvoryBody                        // Tên ca sĩ, mô tả
val TextTertiary = MutedIvory                        // Placeholder, metadata
val TextDisabled = Color(0xFF5A5852)                 // Nội dung vô hiệu / icon placeholder đặc

// ---- Ivory trong suốt: dùng khi nằm trên ảnh bìa / gradient động ------------
// Thang cố định thay cho các Color.White.copy(alpha = …) tuỳ tiện.
val IvoryHairline = PrimaryIvory.copy(alpha = 0.06f) // Viền rất mảnh
val IvorySubtle = PrimaryIvory.copy(alpha = 0.10f)   // Nền nút / chip mờ
val IvoryStroke = PrimaryIvory.copy(alpha = 0.16f)   // Viền, nền nút khi nhấn
val IvoryMuted = PrimaryIvory.copy(alpha = 0.24f)    // Nền được chọn, track slider chưa phát
val IvoryDisabled = PrimaryIvory.copy(alpha = 0.35f) // Icon placeholder, nội dung vô hiệu
val IvoryFaint = PrimaryIvory.copy(alpha = 0.45f)    // Chữ phụ mờ
val IvoryMedium = PrimaryIvory.copy(alpha = 0.60f)   // Chữ phụ, icon chưa chọn
val IvoryHigh = PrimaryIvory.copy(alpha = 0.85f)     // Chữ gần chính

// ---- Bóng đổ & lớp phủ -----------------------------------------------------------
val ShadowColor = ObsidianBlack.copy(alpha = 0.55f)  // ambientColor / spotColor của .shadow()
val ScrimColor = ObsidianBlack.copy(alpha = 0.60f)   // Lớp phủ sau sheet, nền badge trên ảnh, text shadow

// ============================================================================
// Brand & Semantic Accents
// ============================================================================
val Brand = Color(0xFF228B22)                        // Màu thương hiệu: Forest Green
val BrandLight = Color(0xFF2EA02E)                   // Dùng cho chữ trên nền tối (tương phản tốt hơn)
val BrandDark = Color(0xFF1B6E1B)                    // Trạng thái nhấn / nền đậm

val ApexCyan = Color(0xFF00D8F6)                     // Badge Lossless / Hi-Res
val ApexRose = Color(0xFFFA2D48)                     // Xoá / yêu thích / cảnh báo nguy hiểm
val ApexAmber = Color(0xFFF59E0B)                    // Cảnh báo, bit depth cao
val ApexIndigo = Color(0xFF6366F1)                   // Ambient glow (DynamicMeshBackground)

// Hi-Res Audio Metallic Gold (Japan Audio Society)
private val HiResGoldStart = Color(0xFFE5A01A)
private val HiResGoldEnd = Color(0xFFFDC056)
val HiResGoldGradient = Brush.horizontalGradient(listOf(HiResGoldStart, HiResGoldEnd))

// ============================================================================
// Now Playing — nền dự phòng khi không trích được màu từ ảnh bìa (PaletteHelper)
// ============================================================================
val PlayerFallbackTop = SurfaceCard
val PlayerFallbackMid = Color(0xFF1D1B19)
val PlayerFallbackBottom = Color(0xFF171614)

val PlayerBlackAlbumTop = Color(0xFF1D1B18)          // Ảnh bìa gần như đen tuyền
val PlayerBlackAlbumMid = Color(0xFF131210)
val PlayerBlackAlbumBottom = Color(0xFF080706)

val PlayerNeutralAlbumTop = Color(0xFF262420)        // Ảnh bìa trung tính / ít bão hoà
val PlayerNeutralAlbumMid = Color(0xFF1A1916)
val PlayerNeutralAlbumBottom = Color(0xFF0C0B0A)

// ============================================================================
// Monogram & Avatar Palette
// ============================================================================
val AvatarGreen = Color(0xFF2E7D32)
val AvatarPeach = Color(0xFFFF8A65)
val AvatarLime = Color(0xFF9CCC65)
val AvatarPink = Color(0xFFEC407A)
val AvatarIndigo = Color(0xFF5C6BC0)
val AvatarPurple = Color(0xFFAB47BC)
val AvatarOrange = Color(0xFFFFA726)

val AvatarColors = listOf(
    AvatarGreen, AvatarPeach, AvatarLime, AvatarPink, AvatarIndigo, AvatarPurple, AvatarOrange
)

/** Màu avatar ổn định theo tên — dùng chung cho mọi màn hình để cùng một nghệ sĩ luôn cùng màu. */
fun avatarColorFor(name: String): Color = AvatarColors[Math.floorMod(name.hashCode(), AvatarColors.size)]

// ============================================================================
// Haze Frosted Glass Tokens & Extensions (Real-time GPU Glassmorphism)
// ============================================================================
val LocalApexHazeState = compositionLocalOf<HazeState?> { null }
val LocalHazeState = LocalApexHazeState

/**
 * Applies real-time GPU Haze frosted glass blur when HazeState is provided,
 * with pure single-layer sampling to eliminate noise, overdraw, and visual artifacts.
 */
fun Modifier.apexFrostedGlass(
    backgroundColor: Color = SurfaceElevated.copy(alpha = 0.88f),
    blurRadius: androidx.compose.ui.unit.Dp = 20.dp,
    hazeState: HazeState? = null
): Modifier = composed {
    val effectiveHazeState = hazeState ?: LocalApexHazeState.current
    if (effectiveHazeState != null) {
        this.hazeEffect(
            state = effectiveHazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                tint = HazeTint(backgroundColor.copy(alpha = 0.76f)),
                blurRadius = blurRadius,
                noiseFactor = 0f
            )
        )
    } else {
        this.background(backgroundColor)
    }
}

val ApexGlassSurfaceBg = SurfaceElevated.copy(alpha = 0.88f)
val ApexButtonGlassBg = SurfaceElevated.copy(alpha = 0.72f)

// ============================================================================
// Card Tokens & Modifiers
// ============================================================================
val ApexReflectiveBorderBrush = Brush.verticalGradient(
    listOf(
        PrimaryIvory.copy(alpha = 0.08f),
        PrimaryIvory.copy(alpha = 0.04f)
    )
)

val ApexPillBorderBrush = Brush.verticalGradient(
    listOf(
        PrimaryIvory.copy(alpha = 0.18f),
        IvoryHairline
    )
)

/**
 * Standard OneMusic card modifier (SurfaceCard, bo góc 24dp)
 */
fun Modifier.apexGlassCard(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = SurfaceCard,
    borderWidth: Dp = 0.dp,
    borderBrush: Brush = ApexReflectiveBorderBrush,
    elevation: Dp = 0.dp,
    shadowColor: Color = Color.Transparent
): Modifier = this
    .clip(shape)
    .background(backgroundColor)

/**
 * Grouped Card Item Modifier for continuous lists (LazyColumn items).
 * Clean borderless SurfaceIvory grouped container with smooth rounded 24.dp corners.
 */
fun Modifier.apexGroupedCardItem(
    index: Int,
    total: Int,
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color = SurfaceCard,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
): Modifier = composed {
    val shape = when {
        total <= 1 -> RoundedCornerShape(cornerRadius)
        index == 0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        index == total - 1 -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        else -> androidx.compose.ui.graphics.RectangleShape
    }

    this
        .clip(shape)
        .background(backgroundColor)
}

