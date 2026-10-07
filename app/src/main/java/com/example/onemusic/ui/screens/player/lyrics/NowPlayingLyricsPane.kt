package com.example.onemusic.ui.screens.player.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.PillShape

/**
 * Khung Lời bài hát của Now Playing (nhánh LYRICS): thông báo khi không có lời,
 * lời tĩnh (chưa đồng bộ) hoặc lời đồng bộ theo từng chữ.
 *
 * Trạng thái cuộn ([listState], [nestedScrollConnection]) và các hiệu ứng tự cuộn tới câu đang hát
 * nằm ở NowPlayingSheet() vì phải sống qua lúc chuyển sang Hàng đợi. [activeLyricIndex] là lambda
 * để chỉ từng dòng lời đọc giá trị (đổi câu không làm vẽ lại cả khung).
 * Bấm một từ → [onSeekToLine] (chỉ số dòng, vị trí cần tua tới).
 */
@Composable
internal fun NowPlayingLyricsPane(
    track: Track?,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    activeLyricIndex: () -> Int,
    positionState: State<Long>,
    onSeekToLine: (index: Int, positionMs: Long) -> Unit
) {
    val lyrics = track?.lyrics ?: emptyList()
    val isLyricsSynced = remember(lyrics) {
        lyrics.isNotEmpty() && lyrics.any { it.isSynced }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = IvoryDisabled,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = "Không tìm thấy lời bài hát",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = IvoryFaint,
                        fontSize = 15.sp
                    )
                )
            }
        }
    } else if (!isLyricsSynced) {
        // APPLE MUSIC STATIC LYRICS SHEET (Unsynchronized plain text view)
        Box(modifier = Modifier.fillMaxSize()) {
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
                                0.00f to Color.Black,
                                0.82f to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(PillShape)
                            .background(IvorySubtle)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = null,
                            tint = IvoryMedium,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Lời bài hát (Chưa đồng bộ)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IvoryMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "plain_${line.text.hashCode()}_$index" }
                ) { _, line ->
                    if (isInstrumentalLine(line.text)) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(IvoryDisabled)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = IvoryHigh,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                lineHeight = 44.sp,
                                letterSpacing = (-0.4).sp
                            )
                        )
                    }
                }
            }
        }
    } else {
        // APPLE MUSIC TIME-SYNCED & ENHANCED KARAOKE LYRICS VIEW
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val topPadding = 24.dp
            val bottomPadding = 48.dp

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
                                0.00f to Color.Black,
                                0.82f to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = topPadding, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { index, line -> "${line.timestampMs}_$index" }
                ) { index, line ->
                    val isActive = index == activeLyricIndex()
                    val distanceFromActive = kotlin.math.abs(index - activeLyricIndex())
                    WordByWordLyricItem(
                        lyric = line,
                        isActive = isActive,
                        distanceFromActive = distanceFromActive,
                        positionState = positionState,
                        onSeek = { seekTime -> onSeekToLine(index, seekTime) }
                    )
                }
            }
        }
    }
}
