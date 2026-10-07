package com.example.onemusic.ui.screens.player.controls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.onemusic.theme.TextPrimary

/**
 * Biểu tượng 4 cột sóng nhạc động phong cách YouTube Music & One UI.
 * - Nhảy nhấp nhô nhịp nhàng sống động khi isPlaying = true
 * - Tự động hạ thấp xuống mức tối thiểu và đóng băng (idle) khi isPlaying = false
 * - Vẽ trực tiếp bằng Canvas RenderNode GPU 120fps siêu nhẹ, tiết kiệm pin tối đa.
 */
@Composable
fun ApexDynamicEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = TextPrimary,
    barCount: Int = 4,
    barWidth: Dp = 2.8.dp,
    barSpacing: Dp = 2.2.dp,
    maxHeight: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "eq_bars_transition")

    val h1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eq_h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.60f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eq_h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eq_h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(370, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "eq_h4"
    )

    // Khi pause: các cột từ từ hạ xuống mức idle 0.20f
    val animPlayingFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(280),
        label = "eq_playing_factor"
    )

    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val barSpacingPx = with(density) { barSpacing.toPx() }
    val cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)

    androidx.compose.foundation.Canvas(
        modifier = modifier.size(
            width = barWidth * barCount + barSpacing * (barCount - 1),
            height = maxHeight
        )
    ) {
        val totalHeight = size.height
        val heights = listOf(h1, h2, h3, h4)

        heights.forEachIndexed { idx, rawH ->
            val currentH = lerp(0.20f, rawH, animPlayingFactor)
            val barHeightPx = (totalHeight * currentH).coerceIn(barWidthPx, totalHeight)
            val left = idx * (barWidthPx + barSpacingPx)
            val top = (totalHeight - barHeightPx) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = cornerRadius
            )
        }
    }
}

// ============================================================================
// APPLE MUSIC 1:1 QUEUE COMPONENTS (MINI HEADER, MODE PILLS, FLAT TRACK ROW)
// ============================================================================
