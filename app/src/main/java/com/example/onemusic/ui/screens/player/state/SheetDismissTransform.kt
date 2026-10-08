package com.example.onemusic.ui.screens.player.state

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Trượt / thu nhỏ / mờ dần sheet theo kéo-để-đóng; góc dưới bo dần khi kéo xuống.
 * Gọi trong lúc compose (đọc [NowPlayingSheetState.dismissProgress]) như trước khi tách.
 */
internal fun Modifier.sheetDismissTransform(sheetState: NowPlayingSheetState): Modifier {
    val dismissProgress = sheetState.dismissProgress
    val sheetScale = lerp(1f, 0.92f, dismissProgress)
    val sheetAlpha = lerp(1f, 0.82f, dismissProgress)
    val topCornerRadius = lerp(32f, 36f, dismissProgress).dp
    val bottomCornerRadius = lerp(0f, 32f, dismissProgress).dp
    val dynamicSheetShape = RoundedCornerShape(
        topStart = topCornerRadius,
        topEnd = topCornerRadius,
        bottomStart = bottomCornerRadius,
        bottomEnd = bottomCornerRadius
    )

    return this
        .graphicsLayer {
            translationY = sheetState.offsetY.value
            scaleX = sheetScale
            scaleY = sheetScale
            this.alpha = sheetAlpha
            transformOrigin = TransformOrigin(0.5f, 0.95f)
            shape = dynamicSheetShape
            clip = true
            shadowElevation = (28f * (1f - dismissProgress)).coerceAtLeast(0f)
        }
        .clip(dynamicSheetShape)
}
