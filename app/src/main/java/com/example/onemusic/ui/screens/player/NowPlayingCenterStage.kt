package com.example.onemusic.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.screens.player.lyrics.NowPlayingLyricsPane
import com.example.onemusic.ui.screens.player.queue.NowPlayingQueuePane
import com.example.onemusic.ui.screens.player.state.ControlsDeckVisibility
import dev.chrisbanes.haze.HazeState

/**
 * Sân khấu giữa của Now Playing: trống ở chế độ ảnh bìa (để pager ảnh bìa phía sau nhận chạm và hiện tràn viền),
 * hoặc tab Lời / Hàng đợi. Chuyển ngang giữa Lời ↔ Hàng đợi, trồi lên / chìm xuống khi mở / đóng về ảnh bìa.
 */
@Composable
internal fun NowPlayingCenterStage(
    centerView: NowPlayingCenterView,
    track: Track?,
    playbackState: PlaybackState,
    positionState: State<Long>,
    lyricsListState: LazyListState,
    queueListState: LazyListState,
    deckVisibility: ControlsDeckVisibility,
    activeLyricIndex: () -> Int,
    onSeekToLine: (index: Int, positionMs: Long) -> Unit,
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
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = if (centerView == NowPlayingCenterView.ARTWORK) 0.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = centerView,
            transitionSpec = {
                val springSpec = spring<IntOffset>(dampingRatio = 0.84f, stiffness = 320f)
                val horizontalSpringSpec = spring<IntOffset>(dampingRatio = 0.84f, stiffness = 360f)
                val fadeSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
                val fadeOutSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

                when {
                    // 1. Chuyển ngang Parallax giữa LYRICS và QUEUE (Ăn khớp 100% hướng trượt của Island Dock)
                    initialState == NowPlayingCenterView.LYRICS && targetState == NowPlayingCenterView.QUEUE -> {
                        (slideInHorizontally(initialOffsetX = { (it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeIn(animationSpec = fadeSpec))
                            .togetherWith(slideOutHorizontally(targetOffsetX = { (-it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeOut(animationSpec = fadeOutSpec))
                    }
                    initialState == NowPlayingCenterView.QUEUE && targetState == NowPlayingCenterView.LYRICS -> {
                        (slideInHorizontally(initialOffsetX = { (-it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeIn(animationSpec = fadeSpec))
                            .togetherWith(slideOutHorizontally(targetOffsetX = { (it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeOut(animationSpec = fadeOutSpec))
                    }
                    // 2. Đóng về ARTWORK: Chìm êm ái xuống dưới đáy
                    targetState == NowPlayingCenterView.ARTWORK -> {
                        fadeIn(animationSpec = fadeSpec).togetherWith(
                            slideOutVertically(targetOffsetY = { (it * 0.65f).toInt() }, animationSpec = springSpec) + fadeOut(animationSpec = fadeOutSpec)
                        )
                    }
                    // 3. Mở từ ARTWORK lên LYRICS hoặc QUEUE: Trồi vút lên từ dưới đáy màn hình
                    else -> {
                        (slideInVertically(initialOffsetY = { (it * 0.65f).toInt() }, animationSpec = springSpec) + fadeIn(animationSpec = fadeSpec))
                            .togetherWith(fadeOut(animationSpec = fadeOutSpec))
                    }
                }
            },
            label = "center_overlay_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetMode ->
            when (targetMode) {
                NowPlayingCenterView.ARTWORK -> {
                    // Empty transparent space to let the permanently mounted Hero Artwork in Layer 1 receive touches and display 100% full bleed
                    Box(modifier = Modifier.fillMaxSize())
                }
                NowPlayingCenterView.LYRICS -> {
                    NowPlayingLyricsPane(
                        track = track,
                        listState = lyricsListState,
                        nestedScrollConnection = deckVisibility.lyricsNestedScrollConnection,
                        activeLyricIndex = activeLyricIndex,
                        positionState = positionState,
                        onSeekToLine = onSeekToLine
                    )
                }
                NowPlayingCenterView.QUEUE -> {
                    NowPlayingQueuePane(
                        playbackState = playbackState,
                        listState = queueListState,
                        nestedScrollConnection = deckVisibility.queueNestedScrollConnection,
                        hazeState = hazeState,
                        onPlayQueueIndex = onPlayQueueIndex,
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                        onClearPlaybackHistory = onClearPlaybackHistory,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = onAddToPlaylist,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onToggleAutoplay = onToggleAutoplay,
                        onOpenSpeedMenu = onOpenSpeedMenu,
                        onOpenSleepTimer = onOpenSleepTimer,
                        onOpenDetails = onOpenDetails,
                        onOpenOptions = onOpenOptions
                    )
                }
            }
        }
    }
}
