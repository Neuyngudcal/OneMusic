package com.example.onemusic.ui.screens.player.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.example.onemusic.ui.screens.player.NowPlayingCenterView

/**
 * Ẩn/hiện cụm điều khiển (tên bài, thanh tua, nút phát, dock) khi cuộn Lời bài hát / Hàng đợi:
 * cuộn xuống dưới thì ẩn, cuộn ngược lên thì hiện, ở đầu danh sách luôn hiện.
 */
@Stable
internal class ControlsDeckVisibility(
    lyricsListState: LazyListState,
    queueListState: LazyListState,
) {
    // Dynamic Playback Deck Visibility on Lyrics Scroll: Giống bên danh sách chờ (Cuộn xuống dưới ẩn, cuộn lên trên hiện, ở đầu trang luôn hiện)
    var isLyricsDeckVisible by mutableStateOf(true)
    private val isAtLyricsTopState: State<Boolean> = derivedStateOf {
        lyricsListState.firstVisibleItemIndex == 0 && lyricsListState.firstVisibleItemScrollOffset <= 30
    }
    val isAtLyricsTop: Boolean get() = isAtLyricsTopState.value

    // Dynamic Playback Deck Visibility on Queue Scroll: Cuộn xuống dưới để xem thêm bài thì ẨN cụm playing, cuộn lên trên thì HIỆN cụm playing
    var isQueueDeckVisible by mutableStateOf(true)
    private val isAtQueueTopState: State<Boolean> = derivedStateOf {
        queueListState.firstVisibleItemIndex == 0 && queueListState.firstVisibleItemScrollOffset <= 15
    }
    val isAtQueueTop: Boolean get() = isAtQueueTopState.value

    val lyricsNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val deltaY = available.y
            if (source == NestedScrollSource.UserInput) {
                // Kéo lên (cuộn xuống dưới để đọc tiếp lời): ẩn cụm playing
                if (deltaY < -12f) {
                    if (isLyricsDeckVisible && !isAtLyricsTop) {
                        isLyricsDeckVisible = false
                    }
                }
                // Kéo xuống (cuộn ngược lên trên): hiện cụm playing
                else if (deltaY > 12f) {
                    if (!isLyricsDeckVisible) {
                        isLyricsDeckVisible = true
                    }
                }
            }
            return Offset.Zero
        }
    }

    val queueNestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val deltaY = available.y
            // Kéo lên (cuộn xuống dưới để xem bài): ẩn cụm playing
            if (deltaY < -12f) {
                if (isQueueDeckVisible && !isAtQueueTop) {
                    isQueueDeckVisible = false
                }
            }
            // Kéo xuống (cuộn ngược lên trên): hiện cụm playing
            else if (deltaY > 12f) {
                if (!isQueueDeckVisible) {
                    isQueueDeckVisible = true
                }
            }
            return Offset.Zero
        }
    }

    fun isVisible(centerView: NowPlayingCenterView): Boolean = when (centerView) {
        NowPlayingCenterView.LYRICS -> isLyricsDeckVisible || isAtLyricsTop
        NowPlayingCenterView.QUEUE -> isQueueDeckVisible || isAtQueueTop
        NowPlayingCenterView.ARTWORK -> true
    }
}

/**
 * Tạo [ControlsDeckVisibility] và chạy các effect hiện lại cụm điều khiển khi về đầu danh sách
 * hoặc khi vừa chuyển sang tab Lời / Hàng đợi.
 */
@Composable
internal fun rememberControlsDeckVisibility(
    lyricsListState: LazyListState,
    queueListState: LazyListState,
    centerView: NowPlayingCenterView,
): ControlsDeckVisibility {
    val visibility = remember { ControlsDeckVisibility(lyricsListState, queueListState) }

    LaunchedEffect(visibility.isAtLyricsTop) {
        if (visibility.isAtLyricsTop) {
            visibility.isLyricsDeckVisible = true
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.LYRICS) {
            visibility.isLyricsDeckVisible = true
        }
    }

    LaunchedEffect(visibility.isAtQueueTop) {
        if (visibility.isAtQueueTop) {
            visibility.isQueueDeckVisible = true
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.QUEUE) {
            visibility.isQueueDeckVisible = true
        }
    }

    return visibility
}
