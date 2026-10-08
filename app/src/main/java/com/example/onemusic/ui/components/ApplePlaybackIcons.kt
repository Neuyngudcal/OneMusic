package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.example.onemusic.R

/**
 * Bespoke Apple Music style Pause icon.
 * Composed of two distinct vertical rounded pills/capsules with 100% semicircular rounded caps.
 */
@Composable
fun ApplePauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = tint.takeOrElse { AppTheme.colors.textPrimary }
    Canvas(modifier = modifier) {
        val h = size.height
        val pillW = h * 0.34f
        val gap = pillW * 0.46f
        val totalW = pillW * 2f + gap
        val startX = (size.width - totalW) / 2f
        val startY = (size.height - h) / 2f
        val r = pillW / 2f

        // Left Pill
        drawRoundRect(
            color = resolvedTint,
            topLeft = Offset(startX, startY),
            size = Size(pillW, h),
            cornerRadius = CornerRadius(r, r)
        )
        // Right Pill
        drawRoundRect(
            color = resolvedTint,
            topLeft = Offset(startX + pillW + gap, startY),
            size = Size(pillW, h),
            cornerRadius = CornerRadius(r, r)
        )
    }
}

/**
 * Bespoke Apple Music style Play icon (SF Symbol play.fill).
 * High-precision rounded triangle pointing right with filleted vertices and optical center correction.
 */
@Composable
fun ApplePlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = tint.takeOrElse { AppTheme.colors.textPrimary }
    Canvas(modifier = modifier) {
        val h = size.height
        val w = h * 0.84f
        // Optical center shift: triangle centroid is at 1/3, shifting slightly right centers it visually
        val opticalShiftX = w * 0.08f
        val startX = (size.width - w) / 2f + opticalShiftX
        val startY = (size.height - h) / 2f
        val rCorner = h * 0.095f
        val rTip = h * 0.085f

        val path = Path().apply {
            // Start at top-left corner after corner arc
            moveTo(startX, startY + rCorner)
            // Left vertical base line down
            lineTo(startX, startY + h - rCorner)
            // Bottom-left corner arc
            quadraticTo(
                startX, startY + h,
                startX + rCorner * 1.3f, startY + h - rCorner * 0.4f
            )
            // Bottom diagonal line toward tip
            val tipX = startX + w
            val tipY = startY + h / 2f
            lineTo(tipX - rTip * 1.5f, tipY + rTip * 0.86f)
            // Tip rounded arc
            quadraticTo(
                tipX, tipY,
                tipX - rTip * 1.5f, tipY - rTip * 0.86f
            )
            // Top diagonal line toward top-left corner
            lineTo(startX + rCorner * 1.3f, startY + rCorner * 0.4f)
            // Top-left corner arc
            quadraticTo(
                startX, startY,
                startX, startY + rCorner
            )
            close()
        }
        drawPath(path = path, color = resolvedTint)
    }
}

/**
 * Bespoke Apple Music style Backward icon (SF Symbol backward.fill).
 * Two continuous, overlapping rounded triangles pointing left with Apple curvature.
 */
@Composable
fun AppleBackwardIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = tint.takeOrElse { AppTheme.colors.textPrimary }
    Canvas(modifier = modifier) {
        val h = size.height
        val totalW = h * 1.76f
        val triW = totalW / 2f
        val startX = (size.width - totalW) / 2f
        val startY = (size.height - h) / 2f
        val rCorner = h * 0.12f
        val rTip = h * 0.10f

        for (i in 0..1) {
            val offsetX = startX + i * triW
            val rightX = offsetX + triW
            val tipX = offsetX
            val tipY = startY + h / 2f

            val path = Path().apply {
                // Top-right corner after arc
                moveTo(rightX, startY + rCorner)
                // Right vertical base line down
                lineTo(rightX, startY + h - rCorner)
                // Bottom-right corner arc
                quadraticTo(
                    rightX, startY + h,
                    rightX - rCorner * 1.3f, startY + h - rCorner * 0.4f
                )
                // Bottom diagonal line toward tip
                lineTo(tipX + rTip * 1.5f, tipY + rTip * 0.86f)
                // Tip rounded arc
                quadraticTo(
                    tipX, tipY,
                    tipX + rTip * 1.5f, tipY - rTip * 0.86f
                )
                // Top diagonal line toward top-right corner
                lineTo(rightX - rCorner * 1.3f, startY + rCorner * 0.4f)
                // Top-right corner arc
                quadraticTo(
                    rightX, startY,
                    rightX, startY + rCorner
                )
                close()
            }
            drawPath(path = path, color = resolvedTint)
        }
    }
}

/**
 * Bespoke Apple Music style Forward icon (SF Symbol forward.fill).
 * Horizontally mirrored AppleBackwardIcon for mathematical symmetry.
 */
@Composable
fun AppleForwardIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = tint.takeOrElse { AppTheme.colors.textPrimary }
    Box(
        modifier = modifier.graphicsLayer(scaleX = -1f),
        contentAlignment = Alignment.Center
    ) {
        AppleBackwardIcon(
            modifier = Modifier.fillMaxSize(),
            tint = resolvedTint
        )
    }
}

/**
 * Bespoke Apple Music style Lossless audio wave icon.
 * Vector wave graphic representing Apple Lossless audio fidelity.
 */
@Composable
fun AppleLosslessIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = tint.takeOrElse { AppTheme.colors.textPrimary }
    Icon(
        painter = painterResource(id = R.drawable.ic_apple_lossless),
        contentDescription = null,
        tint = resolvedTint,
        modifier = modifier
    )
}
