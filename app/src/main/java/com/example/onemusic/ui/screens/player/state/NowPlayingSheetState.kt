package com.example.onemusic.ui.screens.player.state

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.onemusic.haptics.ApexHapticEngine
import com.example.onemusic.haptics.rememberApexHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Trạng thái kéo-để-đóng của Now Playing: vị trí trượt của sheet, hoạt ảnh vào/ra,
 * ngưỡng đóng + rung khi vượt ngưỡng, và nested scroll khi ở chế độ ảnh bìa.
 */
@Stable
internal class NowPlayingSheetState(
    val offsetY: Animatable<Float, AnimationVector1D>,
    private val screenHeightPx: Float,
    private val slideSpec: AnimationSpec<Float>,
    private val scope: CoroutineScope,
    private val hapticEngine: ApexHapticEngine,
    private val view: View,
    private val onCollapse: State<() -> Unit>,
    private val isArtworkMode: State<() -> Boolean>,
) {
    var isDismissing by mutableStateOf(false)
        private set

    private var hasFiredThresholdHaptic by mutableStateOf(false)

    private val dismissThresholdPx = screenHeightPx * 0.15f

    val dismissProgress: Float
        get() = (offsetY.value / screenHeightPx).coerceIn(0f, 1f)

    suspend fun animateIn() {
        offsetY.animateTo(0f, animationSpec = slideSpec)
    }

    fun collapse(initialVelocity: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        try {
            hapticEngine.performCrispTap(scale = 0.40f, fallbackView = view)
        } catch (_: Exception) {}
        scope.launch {
            offsetY.animateTo(
                targetValue = screenHeightPx,
                initialVelocity = initialVelocity.coerceAtLeast(0f),
                animationSpec = slideSpec
            )
            onCollapse.value()
        }
    }

    /** Nhả tay mà chưa đủ ngưỡng đóng → bật sheet về vị trí mở. */
    private suspend fun settleOpen(velocityY: Float) {
        offsetY.animateTo(
            targetValue = 0f,
            initialVelocity = velocityY.coerceAtMost(0f),
            animationSpec = slideSpec
        )
    }

    private fun updateThresholdHaptic(newOffset: Float) {
        if (!hasFiredThresholdHaptic && newOffset >= dismissThresholdPx) {
            try {
                hapticEngine.performSpringLatch(scale = 0.45f, fallbackView = view)
            } catch (_: Exception) {}
            hasFiredThresholdHaptic = true
        } else if (hasFiredThresholdHaptic && newOffset < dismissThresholdPx * 0.8f) {
            hasFiredThresholdHaptic = false
        }
    }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val currentOffset = offsetY.value
            if (currentOffset > 0f && available.y < 0f && isArtworkMode.value()) {
                val newOffset = (currentOffset + available.y).coerceAtLeast(0f)
                val consumedY = newOffset - currentOffset
                scope.launch { offsetY.snapTo(newOffset) }
                return Offset(0f, consumedY)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y > 0f && !isDismissing && isArtworkMode.value()) {
                val newOffset = (offsetY.value + available.y).coerceAtLeast(0f)
                scope.launch { offsetY.snapTo(newOffset) }
                updateThresholdHaptic(newOffset)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offsetY.value > 0f && !isDismissing && isArtworkMode.value()) {
                val velocityY = available.y
                val shouldDismiss = when {
                    velocityY > 800f -> true
                    velocityY < -800f -> false
                    offsetY.value > dismissThresholdPx -> true
                    else -> false
                }
                if (shouldDismiss) {
                    collapse(initialVelocity = velocityY)
                } else {
                    settleOpen(velocityY)
                }
                return available
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (available.y > 0f && !isDismissing && isArtworkMode.value()) {
                val velocityY = available.y
                val shouldDismiss = when {
                    velocityY > 800f -> true
                    offsetY.value > dismissThresholdPx -> true
                    else -> false
                }
                if (shouldDismiss) {
                    collapse(initialVelocity = velocityY)
                } else {
                    settleOpen(velocityY)
                }
                return available
            }
            return Velocity.Zero
        }
    }

    /**
     * Kéo trực tiếp bằng tay (không qua nested scroll). Đặt ở PointerEventPass.Initial để bắt
     * cử chỉ trước các con; vuốt ngang (pager ảnh bìa) sẽ khóa không cho kéo sheet.
     */
    fun dragToDismissModifier(enabled: Boolean, isPagerScrolling: () -> Boolean): Modifier =
        if (!enabled) Modifier else Modifier.pointerInput(enabled) {
            val velocityTracker = VelocityTracker()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)
                hasFiredThresholdHaptic = false
                var isDragging = false
                var isHorizontalLocked = false
                var totalDx = 0f
                var totalDy = 0f
                // Giữ vị trí trong biến cục bộ của cử chỉ: snapTo chạy bất đồng bộ nên đọc offsetY.value
                // ngay sau khi launch sẽ ra giá trị cũ → cộng dồn lệch, sheet bị rung
                var localOffset = offsetY.value

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val dragChange = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!dragChange.pressed) {
                        break
                    }

                    velocityTracker.addPosition(dragChange.uptimeMillis, dragChange.position)
                    val delta = dragChange.positionChange()
                    totalDx += kotlin.math.abs(delta.x)
                    totalDy += delta.y

                    // Phân luồng cử chỉ (Directional Disambiguation): Nếu người dùng đang vuốt ngang thì khóa không cho kéo sheet
                    if (!isDragging && !isHorizontalLocked) {
                        if (totalDx > 12f && totalDx > totalDy * 1.1f) {
                            isHorizontalLocked = true
                        } else if (totalDy > 16f && totalDy > totalDx * 1.8f && !isPagerScrolling()) {
                            isDragging = true
                        }
                    }

                    if (isDragging && !isHorizontalLocked && !isDismissing) {
                        dragChange.consume()
                        if (delta.y > 0 || localOffset > 0f) {
                            localOffset = (localOffset + delta.y).coerceAtLeast(0f)
                            val newOffset = localOffset
                            scope.launch {
                                offsetY.snapTo(newOffset)
                            }
                            updateThresholdHaptic(newOffset)
                        }
                    }
                }

                if (isDragging && !isDismissing) {
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
                            collapse(initialVelocity = velocityY)
                        } else {
                            settleOpen(velocityY)
                        }
                    }
                }
            }
        }
}

/**
 * Tạo [NowPlayingSheetState] và chạy hoạt ảnh trượt lên khi sheet vừa mở.
 * [isArtworkMode] được đọc lại mỗi lần cuộn/hất nên phải là lambda đọc state, không phải giá trị chụp sẵn.
 */
@Composable
internal fun rememberNowPlayingSheetState(
    onCollapse: () -> Unit,
    isArtworkMode: () -> Boolean,
): NowPlayingSheetState {
    val hapticEngine = rememberApexHaptics()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val currentOnCollapse = rememberUpdatedState(onCollapse)
    val currentIsArtworkMode = rememberUpdatedState(isArtworkMode)

    val state = remember {
        NowPlayingSheetState(
            offsetY = Animatable(screenHeightPx),
            screenHeightPx = screenHeightPx,
            slideSpec = spring(
                dampingRatio = 0.90f,
                stiffness = 280f
            ),
            scope = scope,
            hapticEngine = hapticEngine,
            view = view,
            onCollapse = currentOnCollapse,
            isArtworkMode = currentIsArtworkMode,
        )
    }

    // Smooth Entrance from Bottom
    LaunchedEffect(Unit) {
        state.animateIn()
    }

    return state
}
