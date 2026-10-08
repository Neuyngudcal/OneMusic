package com.example.onemusic.ui.screens.player.state

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.screens.player.NowPlayingCenterView

/**
 * Tự cuộn ở tab Lời / Hàng đợi của Now Playing:
 * - vừa mở tab Lời → nhảy tới câu đang hát; vừa mở Hàng đợi → nhảy tới bài đang phát;
 * - sang câu mới → cuộn định tâm, trừ khi người dùng đang/vừa tự cuộn để đọc (3,5 giây);
 * - đổi bài → đưa lời về đầu; đổi bài khi đang xem Hàng đợi → cuộn tới bài mới.
 */
@Stable
internal class CenterViewAutoScroll(
    activeLyricIndexState: State<Int>,
    private val lastLyricsUserScrollTimeMs: MutableLongState
) {
    /** Câu đang hát (-1 = chưa tới câu nào). Chỉ đọc trong lambda / composable con để sheet không vẽ lại mỗi câu. */
    val activeLyricIndex: Int by activeLyricIndexState

    /** Người dùng bấm một câu để tua → bỏ trạng thái "đang đọc" để tự cuộn chạy lại ngay. */
    fun resetLyricsUserScroll() {
        lastLyricsUserScrollTimeMs.longValue = 0L
    }
}

@Composable
internal fun rememberCenterViewAutoScroll(
    track: Track?,
    playbackState: PlaybackState,
    centerView: NowPlayingCenterView,
    positionState: State<Long>,
    lyricsListState: LazyListState,
    queueListState: LazyListState
): CenterViewAutoScroll {
    // derivedStateOf: chỉ báo thay đổi khi SANG CÂU MỚI, không phải mỗi 40ms khi vị trí đổi
    val lyricLines = track?.lyrics.orEmpty()
    val activeLyricIndexState = remember(lyricLines) {
        derivedStateOf {
            // indexOfLast trả về -1 khi chưa tới câu nào (đoạn nhạc dạo) → không dòng nào sáng
            if (lyricLines.isEmpty()) -1
            else lyricLines.indexOfLast { it.timestampMs <= positionState.value + 60L }
        }
    }
    val activeLyricIndex by activeLyricIndexState

    // Theo dõi trạng thái trước đó để phát hiện khoảnh khắc vừa chuyển sang LYRICS hoặc QUEUE
    var previousCenterView by remember { mutableStateOf(centerView) }

    // User drag detection for lyrics to avoid interrupting manual reading
    val isLyricsDragged by lyricsListState.interactionSource.collectIsDraggedAsState()
    val lastLyricsUserScrollTimeMsState = remember { mutableLongStateOf(0L) }
    var lastLyricsUserScrollTimeMs by lastLyricsUserScrollTimeMsState

    // Tính "người dùng đang đọc" từ lúc cuộn DỪNG HẲN (kể cả sau khi hất/fling), không phải lúc nhấc tay
    var isUserScrollingLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(isLyricsDragged) {
        if (isLyricsDragged) {
            isUserScrollingLyrics = true
            lastLyricsUserScrollTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(lyricsListState.isScrollInProgress) {
        if (!lyricsListState.isScrollInProgress && isUserScrollingLyrics) {
            isUserScrollingLyrics = false
            lastLyricsUserScrollTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.LYRICS && previousCenterView != NowPlayingCenterView.LYRICS) {
            // Nhảy ngay lập tức đến câu hát hiện tại vào trọng tâm quang học
            if (track != null && track.lyrics.isNotEmpty() && activeLyricIndex in track.lyrics.indices) {
                lyricsListState.scrollToItem(activeLyricIndex)
            }
        } else if (centerView == NowPlayingCenterView.QUEUE && previousCenterView != NowPlayingCenterView.QUEUE) {
            // Nhảy ngay lập tức đến bài hát đang phát trong hàng đợi
            if (playbackState.queue.isNotEmpty() && playbackState.currentIndex in playbackState.queue.indices) {
                queueListState.scrollToItem((playbackState.currentIndex - 1).coerceAtLeast(0))
            }
        }
        previousCenterView = centerView
    }

    LaunchedEffect(activeLyricIndex) {
        // Chỉ chạy animation cuộn định tâm khi đang ở màn hình Lyrics và người dùng không đang tự cuộn
        if (centerView == NowPlayingCenterView.LYRICS && track != null && track.lyrics.isNotEmpty() && activeLyricIndex in track.lyrics.indices) {
            val isUserReadingAhead = isLyricsDragged || (System.currentTimeMillis() - lastLyricsUserScrollTimeMs < 3500L)
            if (!isUserReadingAhead && !lyricsListState.isScrollInProgress && previousCenterView == NowPlayingCenterView.LYRICS) {
                lyricsListState.animateScrollToItem(activeLyricIndex)
            }
        }
    }

    // Đổi bài → đưa danh sách lời về đầu, bỏ trạng thái "đang đọc" của bài cũ.
    // Bỏ qua lần chạy đầu để không đè lên việc cuộn tới câu đang hát khi mở sheet ở tab Lời.
    var lastLyricsTrackId by remember { mutableStateOf(track?.id) }
    LaunchedEffect(track?.id) {
        if (track?.id != lastLyricsTrackId) {
            lastLyricsTrackId = track?.id
            lyricsListState.scrollToItem(0)
            lastLyricsUserScrollTimeMs = 0L
        }
    }

    LaunchedEffect(playbackState.currentIndex) {
        // Cuộn mượt mà đến bài hát mới khi đổi bài trong lúc đang xem hàng đợi
        if (centerView == NowPlayingCenterView.QUEUE && playbackState.queue.isNotEmpty()) {
            if (!queueListState.isScrollInProgress && previousCenterView == NowPlayingCenterView.QUEUE) {
                queueListState.animateScrollToItem((playbackState.currentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    return remember(activeLyricIndexState, lastLyricsUserScrollTimeMsState) {
        CenterViewAutoScroll(activeLyricIndexState, lastLyricsUserScrollTimeMsState)
    }
}
