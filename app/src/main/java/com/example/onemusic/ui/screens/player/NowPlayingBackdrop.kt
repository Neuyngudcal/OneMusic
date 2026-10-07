package com.example.onemusic.ui.screens.player

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ObsidianBlack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Layer 0: Living Ambient Mesh Canvas (Làm mờ ảnh bìa và dải chuyển sắc sống động)
 */
@Composable
fun NowPlayingBackdrop(
    context: Context,
    displayedTrack: Track?,
    hazeState: HazeState,
    appSettings: AppSettings?,
    animatedTopColor: Color,
    animatedSecondaryColor: Color,
    animatedAccentColor: Color,
    animatedBottomColor: Color,
    blurRadiusAnimated: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (blurRadiusAnimated > 0.5.dp) {
                    Modifier.blur(blurRadiusAnimated)
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(animatedBottomColor)
        ) {
            if (appSettings?.isDynamicMeshBackgroundEnabled != false) {
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
        }

        content()
    }
}

/**
 * LAYER 1B: AUTHENTIC OBSIDIAN FROSTED GLASS VEIL (Phủ voan than chì mờ thấu quang khi vào Lời bài hát / Hàng đợi)
 */
@Composable
fun NowPlayingVeil(
    frostedGlassAlpha: Float,
    modifier: Modifier = Modifier
) {
    if (frostedGlassAlpha > 0.01f) {
        Box(
            modifier = modifier
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
