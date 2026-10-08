package com.example.onemusic.ui.screens.library.tabs

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.model.Track
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.ui.screens.library.LibraryViewMode
import com.example.onemusic.ui.screens.library.items.SongGridItem
import com.example.onemusic.ui.screens.library.items.SongListItem
import com.example.onemusic.ui.utils.apexBounceClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extension for rendering Songs tab content inside LibraryScreen's LazyList.
 */
fun LazyListScope.songsTabContent(
    sortedTracks: List<Track>,
    currentTrackId: String?,
    libraryViewMode: LibraryViewMode,
    isMultiSelectMode: Boolean,
    selectedTrackIds: Set<String>,
    isHiResBadgeEnabled: Boolean,
    onTrackSelect: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTrackCheckedChange: (String) -> Unit,
    onRescan: () -> Unit
) {
    // 1. Empty state or Tracks List/Grid
    if (sortedTracks.isEmpty()) {
        item(key = "empty_library_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .apexGlassCard(shape = RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Chưa có bài hát nào trong thư viện",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Nhấn nút bên dưới để quét tệp âm thanh trên thiết bị",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.textSecondary)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(AppTheme.colors.textPrimary)
                            .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                                onRescan()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Quét lại bài hát",
                                tint = AppTheme.colors.onInverse,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quét lại bài hát",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AppTheme.colors.onInverse,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    } else {
        when (libraryViewMode) {
            LibraryViewMode.LIST -> {
                itemsIndexed(
                    items = sortedTracks,
                    key = { index, track -> "${track.id}_$index" },
                    contentType = { _, _ -> "track_item" }
                ) { index, track ->
                    val isCurrent = track.id == currentTrackId
                    val isSelectedInBatch = selectedTrackIds.contains(track.id)

                    SongListItem(
                        track = track,
                        isCurrent = isCurrent,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelectedInBatch = isSelectedInBatch,
                        isHiResBadgeEnabled = isHiResBadgeEnabled,
                        index = index,
                        total = sortedTracks.size,
                        onClick = {
                            if (isMultiSelectMode) {
                                onTrackCheckedChange(track.id)
                            } else {
                                onTrackSelect(track)
                            }
                        },
                        onLongClick = { onTrackLongClick(track) },
                        onToggleFavorite = { onToggleFavorite(track.id) }
                    )
                }
            }

            LibraryViewMode.GRID_2 -> {
                val pairs = sortedTracks.chunked(2)
                itemsIndexed(
                    items = pairs,
                    key = { idx, pair -> "lib_g2_${idx}_" + pair.joinToString("_") { it.id } },
                    contentType = { _, _ -> "grid2_row" }
                ) { _, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (track in pair) {
                            val isCurrent = track.id == currentTrackId
                            val isSelectedInBatch = selectedTrackIds.contains(track.id)

                            SongGridItem(
                                track = track,
                                isCurrent = isCurrent,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelectedInBatch = isSelectedInBatch,
                                isHiResBadgeEnabled = isHiResBadgeEnabled,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        onTrackCheckedChange(track.id)
                                    } else {
                                        onTrackSelect(track)
                                    }
                                },
                                onLongClick = { onTrackLongClick(track) },
                                onToggleFavorite = { onToggleFavorite(track.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }


        }
    }
}

/**
 * Drag-to-Select gesture rail on the left edge for rapid multi-item selection.
 */
@Composable
fun SongsDragSelectRail(
    isMultiSelectMode: Boolean,
    sortedTracks: List<Track>,
    selectedTrackIds: Set<String>,
    listState: LazyListState,
    scope: CoroutineScope,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isMultiSelectMode || sortedTracks.isEmpty()) return

    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current

    fun findTrackIndexAtY(y: Float): Int {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val hitItem = visibleItems.firstOrNull { item ->
            y >= item.offset && y <= (item.offset + item.size)
        } ?: return -1
        val key = hitItem.key as? String ?: return -1
        val index = key.substringAfterLast('_').toIntOrNull() ?: return -1
        return if (index in sortedTracks.indices) index else -1
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(80.dp)
            .pointerInput(isMultiSelectMode, sortedTracks) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startIdx = findTrackIndexAtY(down.position.y)
                    var dragStartIndex = startIdx
                    val initialSelection = selectedTrackIds
                    val isSelecting = if (startIdx in sortedTracks.indices) {
                        !initialSelection.contains(sortedTracks[startIdx].id)
                    } else true
                    var lastTickedIdx = startIdx
                    var isDragGesture = false

                    if (startIdx in sortedTracks.indices) {
                        val startTrackId = sortedTracks[startIdx].id
                        val updated = if (isSelecting) initialSelection + startTrackId else initialSelection - startTrackId
                        onSelectionChanged(updated)
                        try {
                            hapticEngine.performGearTick(scale = 0.22f, fallbackView = currentView)
                        } catch (_: Exception) {}
                    }

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }

                        val currentY = change.position.y
                        val dy = kotlin.math.abs(currentY - down.position.y)
                        if (!isDragGesture && dy > 8f) {
                            isDragGesture = true
                        }

                        if (isDragGesture) {
                            change.consume()

                            val currentIdx = findTrackIndexAtY(currentY)
                            if (currentIdx in sortedTracks.indices) {
                                if (dragStartIndex == -1) {
                                    dragStartIndex = currentIdx
                                }
                                if (currentIdx != lastTickedIdx) {
                                    val minIdx = minOf(dragStartIndex, currentIdx)
                                    val maxIdx = maxOf(dragStartIndex, currentIdx)
                                    val rangeIds = (minIdx..maxIdx).map { sortedTracks[it].id }.toSet()
                                    val updated = if (isSelecting) {
                                        initialSelection + rangeIds
                                    } else {
                                        initialSelection - rangeIds
                                    }
                                    onSelectionChanged(updated)
                                    try {
                                        hapticEngine.performGearTick(scale = 0.22f, fallbackView = currentView)
                                    } catch (_: Exception) {}
                                    lastTickedIdx = currentIdx
                                }
                            }

                            // Auto-scroll near edges
                            val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                            if (viewportHeight > 0f) {
                                if (currentY < 180f && listState.canScrollBackward) {
                                    val scrollSpeed = ((180f - currentY) / 180f * 24f).coerceIn(6f, 36f)
                                    scope.launch { listState.scrollBy(-scrollSpeed) }
                                } else if (currentY > viewportHeight - 220f && listState.canScrollForward) {
                                    val scrollSpeed = ((currentY - (viewportHeight - 220f)) / 220f * 24f).coerceIn(6f, 36f)
                                    scope.launch { listState.scrollBy(scrollSpeed) }
                                }
                            }
                        }
                    }
                }
            }
    )
}
