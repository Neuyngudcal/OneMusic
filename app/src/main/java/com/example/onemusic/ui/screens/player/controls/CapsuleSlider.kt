package com.example.onemusic.ui.screens.player.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.TextPrimary

/**
 * Capsule Slider with 120Hz Spring Dynamic Expansion on Touch/Drag
 */
@Composable
fun CapsuleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    height: Dp = 7.dp,
    activeColor: Color = TextPrimary,
    inactiveColor: Color = IvoryMuted
) {
    val hapticEngine = com.example.onemusic.haptics.rememberApexHaptics()
    val currentView = androidx.compose.ui.platform.LocalView.current
    var isInteracting by remember { mutableStateOf(false) }
    var lastHapticFraction by remember { mutableFloatStateOf(value) }
    // pointerInput(Unit) không khởi động lại → đọc callback mới nhất qua rememberUpdatedState
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val animatedHeight by animateDpAsState(
        targetValue = if (isInteracting) height + 3.dp else height,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "capsule_dynamic_height"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 18.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isInteracting = true
                    val width = size.width.toFloat()
                    if (width > 0f) {
                        val frac = (down.position.x / width).coerceIn(0f, 1f)
                        lastHapticFraction = frac
                        currentOnValueChange(frac)
                        hapticEngine.performGearTick(scale = 0.22f, fallbackView = currentView)
                    }

                    val currentPointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == currentPointerId } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        if (width > 0f) {
                            val frac = (change.position.x / width).coerceIn(0f, 1f)
                            if (kotlin.math.abs(frac - lastHapticFraction) >= 0.02f) {
                                hapticEngine.performGearTick(scale = 0.18f, fallbackView = currentView)
                                lastHapticFraction = frac
                            }
                            currentOnValueChange(frac)
                        }
                    }

                    currentOnValueChangeFinished?.invoke()
                    isInteracting = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val activeFraction = value.coerceIn(0f, 1f)
        val activeWidth = maxWidth * activeFraction

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(PillShape)
                .background(inactiveColor)
        ) {
            if (activeFraction > 0f) {
                Box(
                    modifier = Modifier
                        .width(activeWidth)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .background(activeColor)
                )
            }
        }
    }
}
