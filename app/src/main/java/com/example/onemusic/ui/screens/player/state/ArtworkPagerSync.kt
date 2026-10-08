package com.example.onemusic.ui.screens.player.state

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.model.Track
import com.example.onemusic.ui.screens.player.NowPlayingCenterView
import kotlin.math.absoluteValue

/**
 * Pager ảnh bìa (LAYER 1A) đồng bộ hai chiều với bài đang phát:
 * - bài đổi từ bên ngoài (Next/Prev, hàng đợi) → pager cuộn tới trang tương ứng;
 * - người dùng vuốt pager → phát bài ở trang mới khi pager dừng hẳn.
 * Kèm nạp trước ảnh bìa + bảng màu của bài kế tiếp và bài trước đó.
 */
@Composable
internal fun rememberArtworkPagerState(
    queue: List<Track>,
    currentIndex: Int,
    centerView: NowPlayingCenterView,
    onPlayQueueIndex: (Int) -> Unit,
): PagerState {
    val context = LocalContext.current

    // Interactive Horizontal Pager for Seamless Album Artwork Transitions
    val pageCount = if (queue.isNotEmpty()) queue.size else 1
    val initialPage = if (currentIndex in queue.indices) currentIndex else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    val currentPlaybackIndex by rememberUpdatedState(currentIndex)
    val onPlayQueueIndexUpdated by rememberUpdatedState(onPlayQueueIndex)

    // Synchronize external playback changes to Pager with Snappy 280ms Transition
    LaunchedEffect(currentIndex, pageCount, centerView) {
        val target = currentIndex
        if (target in 0 until pageCount && pagerState.currentPage != target) {
            if (centerView == NowPlayingCenterView.ARTWORK) {
                val distance = kotlin.math.abs(pagerState.currentPage - target)
                if (distance <= 2) {
                    pagerState.animateScrollToPage(
                        page = target,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )
                } else {
                    pagerState.scrollToPage(target)
                }
            } else {
                pagerState.scrollToPage(target)
            }
        }
    }

    // Synchronize user horizontal swipe on Pager to PlayerController
    // FIX: Chỉ phát bài mới khi NGƯỜI DÙNG thực sự kéo pager. Trang đổi do code
    // (hàng đợi ngắn lại → pager tự kẹp trang, Next/Prev) không được kích hoạt phát bài.
    val latestQueue by rememberUpdatedState(queue)
    val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var isUserSwipe by remember { mutableStateOf(false) }

    LaunchedEffect(isPagerDragged) {
        if (isPagerDragged) isUserSwipe = true
    }

    // Khi pager dừng hẳn sau một lần người dùng vuốt → phát bài ở trang đó
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && isUserSwipe) {
                isUserSwipe = false
                val page = pagerState.currentPage
                if (page in latestQueue.indices && page != currentPlaybackIndex) {
                    onPlayQueueIndexUpdated(page)
                }
            }
        }
    }

    // Fail-Safe Snap: Đảm bảo pager không bao giờ bị kẹt lửng lơ giữa 2 trang khi thao tác bị gián đoạn
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && pagerState.currentPageOffsetFraction.absoluteValue > 0.001f) {
            pagerState.animateScrollToPage(
                page = pagerState.targetPage,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        }
    }

    // Proactive Preloader: Tự động nạp trước ảnh bìa và trích xuất bảng màu của bài kế tiếp và bài trước đó vào RAM
    LaunchedEffect(currentIndex, queue) {
        if (queue.isNotEmpty()) {
            val curr = currentIndex
            val nextIdx = (curr + 1).coerceAtMost(queue.lastIndex)
            val prevIdx = (curr - 1).coerceAtLeast(0)
            if (nextIdx != curr) {
                queue.getOrNull(nextIdx)?.artworkUrl?.let { com.example.onemusic.ui.utils.preloadArtworkAndColors(context, it) }
            }
            if (prevIdx != curr && prevIdx != nextIdx) {
                queue.getOrNull(prevIdx)?.artworkUrl?.let { com.example.onemusic.ui.utils.preloadArtworkAndColors(context, it) }
            }
        }
    }

    return pagerState
}

/**
 * Bài hiển thị trên Now Playing. Ở chế độ ảnh bìa theo trang pager (targetPage khi đang cuộn) để tên bài
 * đổi cùng lúc với ảnh, không nhảy giữa chừng; ở tab Lời/Hàng đợi là bài đang phát.
 */
internal fun displayedTrackFor(
    centerView: NowPlayingCenterView,
    queue: List<Track>,
    pagerState: PagerState,
    track: Track?
): Track? {
    return if (centerView == NowPlayingCenterView.ARTWORK && queue.isNotEmpty()) {
        val targetIdx = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
        if (targetIdx in queue.indices) queue[targetIdx] else (track ?: queue.firstOrNull())
    } else {
        track ?: (if (queue.isNotEmpty()) queue.firstOrNull() else null)
    }
}
