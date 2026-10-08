package com.example.onemusic.ui.screens.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.screens.player.NowPlayingCenterView
import dev.chrisbanes.haze.HazeState

/**
 * Cụm điều khiển dưới cùng của Now Playing: tên bài (ẩn ở tab Hàng đợi), thanh tua + huy hiệu chất lượng,
 * Trước / Phát-Dừng / Sau và dock Lời – EQ – Hàng đợi. Ẩn khi cuộn xuống trong Lời/Hàng đợi ([visible]).
 * Chiều cao đo được ở chế độ ảnh bìa báo qua [onDeckHeightMeasured] để ảnh bìa chừa chỗ.
 */
@Composable
internal fun ColumnScope.NowPlayingControlsDeck(
    visible: Boolean,
    centerView: NowPlayingCenterView,
    displayedTrack: Track?,
    track: Track?,
    playbackState: PlaybackState,
    positionState: State<Long>,
    appSettings: AppSettings?,
    isEqualizerOpen: Boolean,
    hazeState: HazeState,
    onDeckHeightMeasured: (Int) -> Unit,
    onToggleFavorite: (trackId: String, isCurrentlyFavorite: Boolean) -> Unit,
    onOpenOptions: () -> Unit,
    onOpenTrackDetails: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCenterViewChange: (NowPlayingCenterView) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
            expandFrom = Alignment.Bottom
        ) + fadeIn(tween(200)),
        exit = shrinkVertically(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
            shrinkTowards = Alignment.Bottom
        ) + fadeOut(tween(160))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
                .onSizeChanged { size ->
                    // Chỉ đo ở chế độ ảnh bìa: tab Hàng đợi/Lời ẩn hàng tên bài làm cụm nút thấp đi → ảnh bìa nhảy kích thước
                    if (size.height > 0 && centerView == NowPlayingCenterView.ARTWORK) {
                        onDeckHeightMeasured(size.height)
                    }
                }
        ) {
            // 1. Track Title, Artist & Options Menu (Ẩn khi đang mở Hàng đợi để tối ưu diện tích và tránh lặp thông tin)
            if (centerView != NowPlayingCenterView.QUEUE) {
                NowPlayingTrackHeader(
                    track = displayedTrack,
                    onToggleFavorite = onToggleFavorite,
                    onOpenOptions = onOpenOptions
                )

                Spacer(modifier = Modifier.height(22.dp))
            }

            // Progress Capsule Scrub Bar
            // FIX hiệu năng: thanh tua + 2 nhãn thời gian nằm trong NowPlayingProgressSection,
            // chỉ composable đó đọc positionState → mỗi 40ms chỉ phần này vẽ lại, không phải cả sheet.
            val totalDurMs = if (playbackState.durationMs > 0) playbackState.durationMs else track?.durationMs ?: 0L

            val audioQuality = rememberAudioQualityInfo(displayedTrack)
            val showAudioBadge = (displayedTrack != null) && (appSettings?.isHiResBadgeEnabled != false)

            NowPlayingProgressSection(
                positionState = positionState,
                totalDurMs = totalDurMs,
                hasTrack = track != null,
                onSeek = onSeek,
                centerBadge = {
                    AudioQualityBadge(
                        visible = showAudioBadge,
                        info = audioQuality,
                        onClick = onOpenTrackDetails,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            )

            Spacer(modifier = Modifier.height(43.dp))

            // Master Playback Controls (Apple Music Precision: Prev, Play/Pause, Next)
            MasterPlaybackControls(
                isPlaying = playbackState.isPlaying,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext
            )

            Spacer(modifier = Modifier.height(43.dp))

            // 5. Bottom 3-Action Obsidian Island Dock: Lyrics (♫) - DSP (🎛) - Queue (🄯)
            NowPlayingActionDock(
                centerView = centerView,
                isEqualizerOpen = isEqualizerOpen,
                onSelectLyrics = {
                    onCenterViewChange(NowPlayingCenterView.LYRICS)
                },
                onOpenEqualizer = onOpenEqualizer,
                onSelectQueue = {
                    onCenterViewChange(NowPlayingCenterView.QUEUE)
                },
                onToggleBackToArtwork = {
                    onCenterViewChange(NowPlayingCenterView.ARTWORK)
                },
                hazeState = hazeState
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
