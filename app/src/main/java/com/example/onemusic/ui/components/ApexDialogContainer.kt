package com.example.onemusic.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.onemusic.haptics.ApexHapticEngine
import com.example.onemusic.theme.ApexReflectiveBorderBrush
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.apexFrostedGlass
import dev.chrisbanes.haze.HazeState
import com.example.onemusic.theme.ShadowColor

/**
 * Standard OneMusic Apex Prism Optical Glassmorphic Dialog Container
 *
 * Mandatory Specs (GEMINI.md 3.5):
 * - Real-time GPU Frosted Glass (Haze) with blurRadius = 28.dp, noiseFactor = 0f.
 * - Obsidian Space Glass base: SurfaceElevated.copy(alpha = 0.88f).
 * - AMOLED deep shadow: elevation = 16.dp with Color.Black.copy(alpha = 0.55f).
 * - 2.5D Top-lit Specular Reflection Border: ApexReflectiveBorderBrush with width = 0.85.dp.
 * - Corner Geometry: RoundedCornerShape(28.dp).
 * - Physical Spring Motion: scale 0.90x -> 1.0x (spring(dampingRatio = 0.78f, stiffness = 380f)) + alpha 0f -> 1f (tween(180ms)).
 * - Tactile Micro-Haptics upon presentation.
 */
@Composable
fun ApexDialogContainer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = SurfaceElevated.copy(alpha = 0.88f),
    hazeState: HazeState? = null,
    horizontalMargin: Dp = 20.dp,
    elevation: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val haptics = com.example.onemusic.haptics.rememberApexHaptics()
    val effectiveHazeState = hazeState ?: LocalApexHazeState.current

    val animScale = remember { Animatable(0.90f) }
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        haptics.performItemLift(scale = 0.50f, fallbackView = view)
        animAlpha.animateTo(1f, animationSpec = tween(180))
    }
    LaunchedEffect(Unit) {
        animScale.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .scale(animScale.value)
                    .alpha(animAlpha.value)
                    .shadow(
                        elevation = elevation,
                        shape = shape,
                        ambientColor = ShadowColor,
                        spotColor = ShadowColor
                    )
                    .clip(shape)
                    .apexFrostedGlass(
                        backgroundColor = backgroundColor,
                        blurRadius = 24.dp,
                        hazeState = effectiveHazeState
                    )
                    .border(
                        width = 0.85.dp,
                        brush = ApexReflectiveBorderBrush,
                        shape = shape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                content()
            }
        }
    }
}
