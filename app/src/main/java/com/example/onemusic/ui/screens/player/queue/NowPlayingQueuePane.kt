package com.example.onemusic.ui.screens.player.queue

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Khung Hàng đợi của Now Playing (nhánh QUEUE): header bài đang phát, 3 nút chế độ phát,
 * danh sách "Lịch sử" và "Tiếp tục phát" (vuốt sang trái để xóa, kéo tay nắm để đổi thứ tự).
 *
 * Trạng thái cuộn ([listState], [nestedScrollConnection]) nằm ở NowPlayingSheet() vì phải sống qua
 * lúc chuyển sang Lời bài hát. Các dialog (tốc độ, hẹn giờ, thông tin, menu ⋯) do sheet mở qua callback.
 */
@Composable
internal fun NowPlayingQueuePane(
    playbackState: PlaybackState,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    hazeState: HazeState,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: ((Int, Int) -> Unit)?,
    onRemoveQueueItem: ((Int) -> Unit)?,
    onClearPlaybackHistory: (() -> Unit)?,
    onToggleFavorite: (trackId: String, isCurrentlyFavorite: Boolean) -> Unit,
    onAddToPlaylist: ((Track) -> Unit)?,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: (() -> Unit)?,
    onOpenSpeedMenu: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenOptions: () -> Unit
) {
    val queueItems = playbackState.queue
    if (queueItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = IvoryDisabled,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = "Hàng đợi phát trống",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = IvoryFaint,
                        fontSize = 15.sp
                    )
                )
            }
        }
    } else {
        val currentIndex = playbackState.currentIndex.coerceIn(-1, queueItems.size - 1)
        val currentTrack = playbackState.currentTrack ?: queueItems.getOrNull(currentIndex.coerceAtLeast(0))

        val historyItems = remember(queueItems, currentIndex) {
            if (currentIndex > 0 && currentIndex <= queueItems.size) {
                queueItems.subList(0, currentIndex)
            } else {
                emptyList()
            }
        }

        val upNextItems = remember(queueItems, currentIndex) {
            if (currentIndex in queueItems.indices && currentIndex + 1 < queueItems.size) {
                queueItems.subList(currentIndex + 1, queueItems.size)
            } else if (currentIndex == -1) {
                queueItems
            } else {
                emptyList()
            }
        }
        // Key ổn định theo danh tính bài hát (không phụ thuộc vị trí), tính trước một lần O(n)
        // thay vì mỗi key lặp lại cả danh sách (O(n²)). "occ" = số lần track.id đã xuất hiện trước đó,
        // đảm bảo key duy nhất khi hàng đợi có 2 bài trùng track.id.
        val upNextKeys = remember(upNextItems) {
            val seen = HashMap<String, Int>()
            upNextItems.map { t ->
                val occ = seen.getOrDefault(t.id, 0)
                seen[t.id] = occ + 1
                "next_${t.id}_occ$occ"
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Apple Music Style Mini Playing Header
            QueueTopHeader(
                track = currentTrack,
                isPlaying = playbackState.isPlaying,
                onToggleFavorite = { trkId ->
                    onToggleFavorite(trkId, currentTrack?.isFavorite == true)
                },
                onAddToPlaylist = onAddToPlaylist,
                isShuffle = playbackState.isShuffle,
                onToggleShuffle = onToggleShuffle,
                repeatMode = playbackState.repeatMode,
                onCycleRepeat = onCycleRepeat,
                playbackSpeed = playbackState.playbackSpeed,
                onOpenSpeedMenu = onOpenSpeedMenu,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenDetails = onOpenDetails,
                onOpenOptions = onOpenOptions,
                hazeState = hazeState
            )

            // 2. Playback Modes 3 Pills (Shuffle, Repeat, Autoplay)
            QueuePlaybackModesRow(
                isShuffle = playbackState.isShuffle,
                repeatMode = playbackState.repeatMode,
                isAutoplay = playbackState.isAutoplay,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleAutoplay = { onToggleAutoplay?.invoke() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Scrollable List containing History & Up Next
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.02f to Color.Black,
                                0.92f to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Phân vùng: Lịch sử (History Section)
                if (historyItems.isNotEmpty()) {
                    item(key = "header_history") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lịch sử",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            )

                            // Nút Xóa Lịch Sử
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(IvorySubtle)
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                        onClearPlaybackHistory?.invoke()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xóa",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = historyItems,
                        key = { index, hTrack -> "hist_${hTrack.id}_$index" }
                    ) { hIndex, hTrack ->
                        QueueFlatTrackRow(
                            track = hTrack,
                            showReorder = false,
                            onClick = {
                                onPlayQueueIndex(hIndex)
                            }
                        )
                    }

                    item(key = "divider_history_upnext") {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Phân vùng: Tiếp tục phát (Up Next Section)
                item(key = "header_up_next") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Tiếp tục phát",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 19.sp,
                                letterSpacing = (-0.3).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (playbackState.isAutoplay) "Tự động phát nhạc tương tự" else "Từ danh sách chờ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IvoryFaint,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                if (upNextItems.isEmpty()) {
                    item(key = "empty_up_next") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (playbackState.isAutoplay) "Hết bài hát trong danh sách chờ" else "Danh sách phát kết thúc",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = IvoryFaint,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = upNextItems,
                        // FIX: Key phải ổn định theo danh tính bài hát (không phụ thuộc vị trí),
                        // nếu không Compose sẽ coi mỗi bước kéo-sắp-xếp là 1 item hoàn toàn mới,
                        // làm gãy animation "animateItem" và hủy giữa chừng gesture kéo (pointerInput bị restart).
                        key = { index, _ -> upNextKeys[index] }
                    ) { localIndex, nTrack ->
                        val actualIndex = if (currentIndex in queueItems.indices) currentIndex + 1 + localIndex else localIndex
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
                            // pointerInput(nTrack.id) không khởi động lại → đọc bước kéo mới nhất qua rememberUpdatedState
                            val latestItemStepPx = rememberUpdatedState(itemStepPx)
                            var accumulatedDragY by remember { mutableFloatStateOf(0f) }

                            // FIX: Đọc các giá trị có thể đổi theo vị trí (danh giới hàng đợi)
                            // qua rememberUpdatedState để luôn lấy giá trị mới nhất mà KHÔNG cần khởi động lại
                            // gesture pointerInput giữa chừng khi người dùng đang kéo liên tục nhiều bậc.
                            val latestQueueLastIndex = rememberUpdatedState(queueItems.lastIndex)
                            val latestMinReorderIndex = rememberUpdatedState((playbackState.currentIndex + 1).coerceAtLeast(0))

                            QueueFlatTrackRow(
                                track = nTrack,
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
                                            .pointerInput(nTrack.id) {
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
                }
            }
        }
    }
}
