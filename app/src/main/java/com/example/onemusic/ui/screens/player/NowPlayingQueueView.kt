package com.example.onemusic.ui.screens.player

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.R
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.playback.RepeatMode
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ActivePillBg
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Header thông tin bài đang phát trong hàng đợi
 */
@Composable
fun QueueTopHeader(
    track: Track?,
    isPlaying: Boolean,
    onToggleFavorite: (String) -> Unit = {},
    onAddToPlaylist: ((Track) -> Unit)? = null,
    isShuffle: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    repeatMode: RepeatMode = RepeatMode.OFF,
    onCycleRepeat: () -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onOpenSpeedMenu: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenDetails: () -> Unit = {},
    onOpenOptions: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    if (track == null) return
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Album Artwork (46dp x 46dp, bo góc 12dp)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.artworkUrl)
                        .crossfade(true)
                        .size(128, 128)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = IvoryFaint,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScrimColor),
                    contentAlignment = Alignment.Center
                ) {
                    ApexDynamicEqualizerBars(
                        isPlaying = true,
                        barColor = PrimaryIvory
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track Title & Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = IvoryMedium,
                    fontSize = 13.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Cụm 3 nút chuyển chế độ phát trong Hàng đợi: Shuffle, Repeat, Autoplay
 */
@Composable
fun QueuePlaybackModesRow(
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isAutoplay: Boolean,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shuffle Mode Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isShuffle) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onToggleShuffle()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_widget_shuffle),
                contentDescription = "Trộn bài",
                tint = PrimaryIvory,
                modifier = Modifier.size(20.dp)
            )
        }

        // Repeat Mode Pill
        val isRepeatActive = repeatMode != RepeatMode.OFF
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isRepeatActive) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onCycleRepeat()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Lặp lại",
                tint = PrimaryIvory,
                modifier = Modifier.size(20.dp)
            )
        }

        // Autoplay Infinity Pill (∞)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(PillShape)
                .background(if (isAutoplay) ActivePillBg else IvorySubtle)
                .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                    onToggleAutoplay()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AllInclusive,
                contentDescription = "Tự động phát",
                tint = PrimaryIvory,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Một hàng bài hát trong danh sách Hàng đợi
 */
@Composable
fun QueueFlatTrackRow(
    track: Track,
    showReorder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reorderContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                onClick()
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Artwork (42dp x 42dp, bo góc 10dp)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.artworkUrl)
                        .crossfade(true)
                        .size(128, 128)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = IvoryFaint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = IvoryMedium,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelSmall.copy(
                color = IvoryFaint,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal
            )
        )

        // Reorder handle if requested
        if (showReorder && reorderContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            reorderContent()
        }
    }
}

/**
 * Toàn bộ giao diện Hàng đợi phát nhạc (Apple Music 1:1 Queue View)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingQueueContent(
    playbackState: PlaybackState,
    queueListState: LazyListState,
    queueNestedScrollConnection: NestedScrollConnection,
    onPlayQueueIndex: (Int) -> Unit,
    onMoveQueueItem: ((from: Int, to: Int) -> Unit)?,
    onRemoveQueueItem: ((Int) -> Unit)?,
    onClearPlaybackHistory: (() -> Unit)?,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: ((Track) -> Unit)?,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: (() -> Unit)?,
    onOpenSpeedMenu: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenOptions: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val queueItems = playbackState.queue
    if (queueItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
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

        val upNextKeys = remember(upNextItems) {
            val seen = HashMap<String, Int>()
            upNextItems.map { t ->
                val occ = seen.getOrDefault(t.id, 0)
                seen[t.id] = occ + 1
                "next_${t.id}_occ$occ"
            }
        }

        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // 1. Apple Music Style Mini Playing Header
            QueueTopHeader(
                track = currentTrack,
                isPlaying = playbackState.isPlaying,
                onToggleFavorite = onToggleFavorite,
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
                state = queueListState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(queueNestedScrollConnection)
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

                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(IvoryStroke)
                                    .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                        onClearPlaybackHistory?.invoke()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Xóa",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = IvoryMedium,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = historyItems,
                        key = { hIdx, hTrack -> "hist_${hTrack.id}_$hIdx" }
                    ) { hIndex, hTrack ->
                        QueueFlatTrackRow(
                            track = hTrack,
                            showReorder = false,
                            onClick = {
                                onPlayQueueIndex(hIndex)
                            }
                        )
                    }
                }

                // Phân vùng: Tiếp tục phát (Up Next Section)
                if (upNextItems.isNotEmpty()) {
                    item(key = "header_up_next") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiếp theo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 19.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            )

                            Text(
                                text = "${upNextItems.size} bài hát",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = IvoryFaint,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    itemsIndexed(
                        items = upNextItems,
                        key = { index, _ -> upNextKeys[index] }
                    ) { nextRelIndex, nTrack ->
                        val actualIndex = if (currentIndex == -1) nextRelIndex else (currentIndex + 1 + nextRelIndex)
                        val latestActualIndex = rememberUpdatedState(actualIndex)

                        var isDismissed by remember(nTrack.id) { androidx.compose.runtime.mutableStateOf(false) }

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    isDismissed = true
                                    val targetIdx = latestActualIndex.value
                                    if (targetIdx in queueItems.indices) {
                                        onRemoveQueueItem?.invoke(targetIdx)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = !isDismissed,
                            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
                        ) {
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    val isSwipingToDelete = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSwipingToDelete) Color(0xFFE53935) else Color.Transparent)
                                            .padding(end = 16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (isSwipingToDelete) {
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
                                var rowHeightPx by remember { mutableIntStateOf(0) }
                                val spacingPx = with(density) { 2.dp.toPx() }
                                val itemStepPx = if (rowHeightPx > 0) rowHeightPx + spacingPx else with(density) { 56.dp.toPx() }
                                val latestItemStepPx = rememberUpdatedState(itemStepPx)
                                var accumulatedDragY by remember { mutableFloatStateOf(0f) }

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
}
