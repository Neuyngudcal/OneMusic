package com.example.onemusic.ui.screens.player.queue

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.PrimaryIvory

/**
 * Một hàng trong "Tiếp tục phát" của hàng đợi: vuốt sang trái để xóa, kéo tay nắm để đổi thứ tự.
 *
 * [actualIndex] là vị trí của bài trong cả hàng đợi; [minReorderIndex]..[queueLastIndex] là khoảng được
 * phép kéo tới (không kéo lên trên bài đang phát). Các giá trị này được đọc qua rememberUpdatedState
 * nên cử chỉ đang kéo không bị khởi động lại khi vị trí thay đổi.
 * Là hàm mở rộng của LazyItemScope vì dùng Modifier.animateItem.
 */
@Composable
internal fun LazyItemScope.QueueReorderableRow(
    track: Track,
    actualIndex: Int,
    queueLastIndex: Int,
    minReorderIndex: Int,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: ((Int, Int) -> Unit)?,
    onRemoveQueueItem: ((Int) -> Unit)?
) {
    val currentOnRemove = rememberUpdatedState(onRemoveQueueItem)
    val canDismiss = onRemoveQueueItem != null
    // FIX: Đọc các giá trị có thể đổi theo vị trí (actualIndex) qua rememberUpdatedState
    // để confirmValueChange (bị remember theo key hàng) và gesture kéo luôn dùng vị trí mới nhất.
    val latestActualIndex = rememberUpdatedState(actualIndex)

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.50f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                currentOnRemove.value?.invoke(latestActualIndex.value)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .animateItem(
                fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                fadeOutSpec = tween(200, easing = FastOutSlowInEasing),
                placementSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
            ),
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart &&
                    dismissState.currentValue == SwipeToDismissBoxValue.Settled
            val progress = dismissState.progress

            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ApexRose.copy(alpha = (progress * 1.5f).coerceIn(0f, 1f)))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Xóa khỏi hàng đợi",
                        tint = PrimaryIvory,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) {
        val view = LocalView.current
        val density = LocalDensity.current
        // Bước kéo = chiều cao thật của hàng + khoảng cách 2dp giữa các hàng, để hàng bám đúng ngón tay
        var rowHeightPx by remember { mutableIntStateOf(0) }
        val spacingPx = with(density) { 2.dp.toPx() }
        val itemStepPx = if (rowHeightPx > 0) rowHeightPx + spacingPx else with(density) { 56.dp.toPx() }
        // pointerInput(track.id) không khởi động lại → đọc bước kéo mới nhất qua rememberUpdatedState
        val latestItemStepPx = rememberUpdatedState(itemStepPx)
        var accumulatedDragY by remember { mutableFloatStateOf(0f) }

        // FIX: Đọc các giá trị có thể đổi theo vị trí (danh giới hàng đợi)
        // qua rememberUpdatedState để luôn lấy giá trị mới nhất mà KHÔNG cần khởi động lại
        // gesture pointerInput giữa chừng khi người dùng đang kéo liên tục nhiều bậc.
        val latestQueueLastIndex = rememberUpdatedState(queueLastIndex)
        val latestMinReorderIndex = rememberUpdatedState(minReorderIndex)

        QueueFlatTrackRow(
            track = track,
            showReorder = true,
            onClick = {
                onPlayQueueIndex(actualIndex)
            },
            modifier = Modifier.onSizeChanged { rowHeightPx = it.height },
            reorderContent = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        // FIX: key theo track.id (ổn định) thay vì actualIndex (đổi liên tục khi kéo),
                        // để gesture kéo-sắp-xếp không bị hủy giữa chừng sau mỗi bậc di chuyển.
                        .pointerInput(track.id) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    accumulatedDragY = 0f
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    accumulatedDragY = 0f
                                },
                                onDragCancel = {
                                    accumulatedDragY = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDragY += dragAmount

                                    val currentActualIndex = latestActualIndex.value
                                    val minReorderIndex = latestMinReorderIndex.value
                                    val stepPx = latestItemStepPx.value
                                    if (accumulatedDragY < -stepPx && currentActualIndex > minReorderIndex) {
                                        onMoveQueueItem?.invoke(currentActualIndex, currentActualIndex - 1)
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        accumulatedDragY = 0f
                                    } else if (accumulatedDragY > stepPx && currentActualIndex < latestQueueLastIndex.value) {
                                        onMoveQueueItem?.invoke(currentActualIndex, currentActualIndex + 1)
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        accumulatedDragY = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Reorder,
                        contentDescription = "Kéo để dời thứ tự",
                        tint = IvoryFaint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}
