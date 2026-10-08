package com.example.onemusic.ui.components

import androidx.compose.ui.composed
import com.example.onemusic.theme.AppTheme
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Modifier helper to turn any circular element into a Clean Borderless Obsidian Glass component with Real-time GPU Haze.
 */
fun Modifier.apexCircularGlassButton(
    size: Dp = 44.dp,
    backgroundColor: Color = Color.Unspecified, // Unspecified → surface1 alpha 0.72 theo theme
    elevation: Dp = 8.dp,
    hazeState: HazeState? = null
): Modifier = composed {
    this
        .size(size)
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            ambientColor = AppTheme.colors.shadow,
            spotColor = AppTheme.colors.shadow
        )
        .clip(CircleShape)
        .apexFrostedGlass(
            backgroundColor = backgroundColor.takeOrElse { AppTheme.colors.surface1.copy(alpha = 0.72f) },
            blurRadius = 20.dp,
            hazeState = hazeState
        )
}

/**
 * Standard OneMusic Apex Prism Clean Borderless Circular Glass Button (GEMINI.md 3.6).
 *
 * Sizing & Geometry:
 * - CircleShape, 40dp..46dp
 * - Borderless Clean Obsidian Glass Disc (No harsh cyan outer rim)
 * - Real-time GPU Frosted Glass with Obsidian Space Glass Tint (SurfaceElevated alpha = 0.72f)
 * - 8dp AMOLED drop shadow
 * - Spring Motion & Tactile Haptics (apexBounceClick scaleDown = 0.90f..0.92f)
 */
@Composable
fun ApexCircularGlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    iconTint: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    elevation: Dp = 8.dp,
    scaleDown: Float = 0.90f,
    hazeState: HazeState? = null
) {
    Box(
        modifier = modifier
            .apexCircularGlassButton(
                size = size,
                backgroundColor = backgroundColor,
                elevation = elevation,
                hazeState = hazeState
            )
            .apexBounceClick(scaleDown = scaleDown, enableHaptic = true) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint.takeOrElse { AppTheme.colors.textPrimary },
            modifier = Modifier.size(iconSize)
        )
    }
}
