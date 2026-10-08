package com.example.onemusic.ui.screens.player.backdrop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.screens.player.artwork.HeroArtworkPager
import com.example.onemusic.ui.utils.rememberArtworkColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Nền của Now Playing, nằm dưới lớp điều khiển (LAYER 2). Phát ra 2 lớp con của Box cha:
 * - Box được làm mờ khi rời chế độ ảnh bìa ([isArtworkMode] = false), gồm
 *   LAYER 0 (ảnh bìa phóng to + dải màu lấy từ ảnh bìa) và LAYER 1A ([HeroArtworkPager]),
 *   cả hai nằm trong nguồn [hazeState] để kính lấy mẫu đúng thứ hiển thị phía sau;
 * - LAYER 1B: lớp voan tối hiện dần khi mở Lời bài hát / Hàng đợi.
 */
@Composable
internal fun NowPlayingBackdrop(
    isArtworkMode: Boolean,
    displayedTrack: Track?,
    track: Track?,
    queue: List<Track>,
    pagerState: PagerState,
    isDynamicMeshBackgroundEnabled: Boolean,
    motionVideoPath: String?,
    motionPlayer: androidx.media3.exoplayer.ExoPlayer?,
    hazeState: HazeState,
    isSheetFullyVisible: Boolean,
    controlsDeckHeight: Dp,
    onArtworkLongClick: () -> Unit
) {
    val context = LocalContext.current

    // Dynamic Artwork Colors with Swatch Fallback & HSL (Synchronized with displayedTrack)
    val dynamicArtworkColors = rememberArtworkColors(imageUrl = displayedTrack?.artworkUrl)

    val targetTopColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.topColor.copy(alpha = 1f)
    }
    val targetSecondaryColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.secondaryColor.copy(alpha = 1f)
    }
    val targetAccentColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.accentColor.copy(alpha = 1f)
    }
    val targetBottomColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.bottomColor.copy(alpha = 1f)
    }

    val auroraColorEasing = remember { CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.00f) }

    val animatedTopColor by animateColorAsState(
        targetValue = targetTopColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_top_color"
    )
    val animatedSecondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_secondary_color"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_accent_color"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = targetBottomColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_bottom_color"
    )

    // Quản lý chuyển cảnh Kính Mờ Quang Học Trên Toàn Bộ Giao Diện Now Playing (120fps)
    val blurRadiusAnimated by animateDpAsState(
        targetValue = if (isArtworkMode) 0.dp else 26.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "nowplaying_bg_blur"
    )
    val frostedGlassAlpha by animateFloatAsState(
        targetValue = if (!isArtworkMode) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "frosted_glass_alpha"
    )

    // LAYER 0 & 1A: TRUE NOW PLAYING LIVING BACKDROP (AMBIENT MESH + HERO ALBUM ARTWORK)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (blurRadiusAnimated > 0.5.dp) {
                    Modifier.blur(blurRadiusAnimated)
                } else Modifier
            )
    ) {
        // LAYER 0: Haze Source Background Canvas (Album Art Blur + Vibrant Mesh Gradient)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(animatedBottomColor)
        ) {
            if (isDynamicMeshBackgroundEnabled) {
                // 1. Phóng to ảnh album + làm mờ (Blur 50dp) với hiệu ứng chuyển đổi mờ dần 600ms siêu mượt
                AnimatedContent(
                    targetState = displayedTrack?.artworkUrl,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) togetherWith fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing))
                    },
                    label = "bg_art_blur_crossfade",
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 50.dp)
                ) { artUrl ->
                    if (!artUrl.isNullOrBlank()) {
                        val bgImageRequest = remember(artUrl) {
                            ImageRequest.Builder(context)
                                .data(artUrl)
                                .crossfade(false)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build()
                        }
                        AsyncImage(
                            model = bgImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = 0.45f
                                    scaleX = 1.25f
                                    scaleY = 1.25f
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Black,
                                            0.30f to Color.Black,
                                            0.50f to Color.Black.copy(alpha = 0.85f),
                                            0.70f to Color.Black.copy(alpha = 0.55f),
                                            0.85f to Color.Black.copy(alpha = 0.25f),
                                            0.96f to Color.Black.copy(alpha = 0.05f),
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )
                    }
                }

                // 2. Dynamic Ambient Gradient Mesh (Linear, Radial & Warm Organic Atmosphere)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // A. Linear Gradient trải dài toàn màn hình từ trên xuống theo dải màu hữu cơ
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to animatedTopColor.copy(alpha = 0.85f),
                                    0.35f to animatedTopColor.copy(alpha = 0.75f),
                                    0.65f to animatedSecondaryColor.copy(alpha = 0.78f),
                                    1.00f to animatedBottomColor.copy(alpha = 0.92f)
                                )
                            )

                            // B. Radial Gradient rực rỡ tỏa rộng quanh khu vực đĩa nhạc với màu Accent sống động
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedAccentColor.copy(alpha = 0.45f),
                                        animatedTopColor.copy(alpha = 0.30f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                                    radius = size.height * 0.65f
                                )
                            )

                            // C. Lớp làm dịu nhẹ nhàng ở chân máy để giữ độ tương phản cho phím bấm trắng
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    0.70f to Color.Transparent,
                                    1.00f to Color.Black.copy(alpha = 0.32f)
                                )
                            )
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack)
                )
            }

            // LAYER 1A (nằm trong nguồn Haze để kính lấy mẫu cả ảnh bìa): HERO ALBUM ARTWORK CAROUSEL (FULL BLEED TRÀN VIỀN TỪ ĐỈNH MÁY, ĐỒNG BỘ 100% VỚI CỤM PHÍM ĐÁY)
            HeroArtworkPager(
                queue = queue,
                track = track,
                pagerState = pagerState,
                motionVideoPath = motionVideoPath,
                motionPlayer = motionPlayer,
                hazeState = hazeState,
                isSheetFullyVisible = isSheetFullyVisible,
                scrimColor = if (isDynamicMeshBackgroundEnabled) animatedSecondaryColor else ObsidianBlack,
                controlsDeckHeight = controlsDeckHeight,
                onArtworkLongClick = onArtworkLongClick
            )
        }
    }

    // LAYER 1B: AUTHENTIC OBSIDIAN FROSTED GLASS VEIL (Phủ voan than chì mờ thấu quang khi vào Lời bài hát / Hàng đợi)
    if (frostedGlassAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = frostedGlassAlpha }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )
    }
}
