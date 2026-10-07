package com.example.onemusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import com.example.onemusic.haptics.rememberApexHaptics
import kotlinx.coroutines.launch

/**
 * OneMusic Apex Prism Spring Physics Specs for 120Hz fluid touch and gesture response
 */
val ApexSpringPress: AnimationSpec<Float> = spring(
    dampingRatio = 0.85f,
    stiffness = 300f
)

val ApexSpringRelease: AnimationSpec<Float> = spring(
    dampingRatio = 0.82f,
    stiffness = 280f
)

val ApexSpringFast: AnimationSpec<Float> = spring(
    dampingRatio = 0.86f,
    stiffness = 360f
)

/**
 * Reusable OneMusic Apex Prism Spring Bounce Touch Modifier supporting Tap and Long Press.
 * Optimized with Compose InteractionSource for zero-jank 120Hz fling & scroll performance.
 *
 * @param role vai trò báo cho TalkBack (mặc định "Nút"); truyền null nếu phần tử không phải nút.
 * @param onClickLabel mô tả hành động khi nhấn đúp (TalkBack đọc "Nhấn đúp để <onClickLabel>").
 * @param onLongClickLabel mô tả hành động khi nhấn giữ.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.apexBounceClick(
    scaleDown: Float = 0.95f,
    enableHaptic: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (onClick == null && onLongClick == null) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "apex_bounce_scale"
    )
    val hapticEngine = rememberApexHaptics()
    val currentView = androidx.compose.ui.platform.LocalView.current

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = role,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onClick = {
                if (enableHaptic) {
                    try {
                        hapticEngine.performCrispTap(scale = 0.35f, fallbackView = currentView)
                    } catch (_: Exception) {}
                }
                onClick?.invoke()
            },
            onLongClick = onLongClick?.let { longClickAction ->
                {
                    if (enableHaptic) {
                        try {
                            hapticEngine.performItemLift(scale = 0.70f, fallbackView = currentView)
                        } catch (_: Exception) {}
                    }
                    longClickAction.invoke()
                }
            }
        )
}

/**
 * Reusable press-scale modifier without hijacking click events (useful when wrapping existing clickables).
 */
fun Modifier.apexPressScale(
    scaleDown: Float = 0.96f
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(scaleDown) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                scope.launch {
                    scale.animateTo(scaleDown, animationSpec = ApexSpringPress)
                }
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                scope.launch {
                    scale.animateTo(1f, animationSpec = ApexSpringRelease)
                }
            }
        }
}
