package com.example.onemusic.ui.screens.player

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import com.example.onemusic.haptics.ApexHapticEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * NestedScrollConnection xử lý vuốt từ trên xuống của sheet khi đang ở chế độ ARTWORK
 */
@Composable
fun rememberSheetNestedScrollConnection(
    sheetOffsetY: Animatable<Float, *>,
    isDismissing: () -> Boolean,
    centerView: () -> NowPlayingCenterView,
    dismissThresholdPx: Float,
    sheetSlideSpec: AnimationSpec<Float>,
    scope: CoroutineScope,
    hapticEngine: ApexHapticEngine,
    currentView: View,
    onCollapse: (initialVelocity: Float) -> Unit,
    hasFiredThresholdHaptic: () -> Boolean,
    setHasFiredThresholdHaptic: (Boolean) -> Unit
): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = sheetOffsetY.value
                if (currentOffset > 0f && available.y < 0f && centerView() == NowPlayingCenterView.ARTWORK) {
                    val newOffset = (currentOffset + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - currentOffset
                    scope.launch { sheetOffsetY.snapTo(newOffset) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && !isDismissing() && centerView() == NowPlayingCenterView.ARTWORK) {
                    val newOffset = (sheetOffsetY.value + available.y).coerceAtLeast(0f)
                    scope.launch { sheetOffsetY.snapTo(newOffset) }

                    if (!hasFiredThresholdHaptic() && newOffset >= dismissThresholdPx) {
                        try {
                            hapticEngine.performSpringLatch(scale = 0.45f, fallbackView = currentView)
                        } catch (_: Exception) {}
                        setHasFiredThresholdHaptic(true)
                    } else if (hasFiredThresholdHaptic() && newOffset < dismissThresholdPx * 0.8f) {
                        setHasFiredThresholdHaptic(false)
                    }

                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffsetY.value > 0f && !isDismissing() && centerView() == NowPlayingCenterView.ARTWORK) {
                    val velocityY = available.y
                    val shouldDismiss = when {
                        velocityY > 800f -> true
                        velocityY < -800f -> false
                        sheetOffsetY.value > dismissThresholdPx -> true
                        else -> false
                    }
                    if (shouldDismiss) {
                        onCollapse(velocityY)
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityY.coerceAtMost(0f),
                            animationSpec = sheetSlideSpec
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f && !isDismissing() && centerView() == NowPlayingCenterView.ARTWORK) {
                    val velocityY = available.y
                    val shouldDismiss = when {
                        velocityY > 800f -> true
                        sheetOffsetY.value > dismissThresholdPx -> true
                        else -> false
                    }
                    if (shouldDismiss) {
                        onCollapse(velocityY)
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityY.coerceAtMost(0f),
                            animationSpec = sheetSlideSpec
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }
}

/**
 * Modifier xử lý cử chỉ kéo xuống để đóng sheet (Drag-to-dismiss với Directional Disambiguation)
 */
fun Modifier.sheetDragToDismiss(
    enabled: Boolean = true,
    sheetOffsetY: Animatable<Float, *>,
    isDismissing: () -> Boolean,
    isScrollInProgress: () -> Boolean,
    dismissThresholdPx: Float,
    sheetSlideSpec: AnimationSpec<Float>,
    scope: CoroutineScope,
    hapticEngine: ApexHapticEngine,
    currentView: View,
    onCollapse: (initialVelocity: Float) -> Unit,
    setHasFiredThresholdHaptic: (Boolean) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled) {
    val velocityTracker = VelocityTracker()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        velocityTracker.resetTracking()
        velocityTracker.addPosition(down.uptimeMillis, down.position)
        setHasFiredThresholdHaptic(false)
        var isDragging = false
        var isHorizontalLocked = false
        var totalDx = 0f
        var totalDy = 0f
        var localOffset = sheetOffsetY.value

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val dragChange = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!dragChange.pressed) {
                break
            }

            velocityTracker.addPosition(dragChange.uptimeMillis, dragChange.position)
            val delta = dragChange.positionChange()
            totalDx += abs(delta.x)
            totalDy += delta.y

            if (!isDragging && !isHorizontalLocked) {
                if (totalDx > 12f && totalDx > totalDy * 1.1f) {
                    isHorizontalLocked = true
                } else if (totalDy > 16f && totalDy > totalDx * 1.8f && !isScrollInProgress()) {
                    isDragging = true
                }
            }

            if (isDragging && !isHorizontalLocked && !isDismissing()) {
                dragChange.consume()
                if (delta.y > 0 || localOffset > 0f) {
                    localOffset = (localOffset + delta.y).coerceAtLeast(0f)
                    val newOffset = localOffset
                    scope.launch {
                        sheetOffsetY.snapTo(newOffset)
                    }

                    if (newOffset >= dismissThresholdPx) {
                        try {
                            hapticEngine.performSpringLatch(scale = 0.45f, fallbackView = currentView)
                        } catch (_: Exception) {}
                        setHasFiredThresholdHaptic(true)
                    } else if (newOffset < dismissThresholdPx * 0.8f) {
                        setHasFiredThresholdHaptic(false)
                    }
                }
            }
        }

        if (isDragging && !isDismissing()) {
            val velocityY = velocityTracker.calculateVelocity().y
            val currentOffset = localOffset
            val shouldDismiss = when {
                velocityY > 800f -> true
                velocityY < -800f -> false
                currentOffset > dismissThresholdPx -> true
                else -> false
            }

            scope.launch {
                if (shouldDismiss) {
                    onCollapse(velocityY)
                } else {
                    sheetOffsetY.animateTo(
                        targetValue = 0f,
                        initialVelocity = velocityY.coerceAtMost(0f),
                        animationSpec = sheetSlideSpec
                    )
                }
            }
        }
    }
}
