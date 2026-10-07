package com.example.onemusic.ui.components

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
import com.example.onemusic.theme.ApexButtonGlassBg
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import com.example.onemusic.theme.ShadowColor
import com.example.onemusic.theme.SurfaceElevated

/**
 * Modifier helper to turn any circular element into a Clean Borderless Obsidian Glass component with Real-time GPU Haze.
 */
fun Modifier.apexCircularGlassButton(
    size: Dp = 44.dp,
    backgroundColor: Color = ApexButtonGlassBg,
    elevation: Dp = 8.dp,
    hazeState: HazeState? = null
): Modifier = this
    .size(size)
    .shadow(
        elevation = elevation,
        shape = CircleShape,
        ambientColor = ShadowColor,
        spotColor = ShadowColor
    )
    .clip(CircleShape)
    .apexFrostedGlass(
        backgroundColor = backgroundColor,
        blurRadius = 20.dp,
        hazeState = hazeState
    )

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
    iconTint: Color = TextPrimary,
    backgroundColor: Color = ApexButtonGlassBg,
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
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
