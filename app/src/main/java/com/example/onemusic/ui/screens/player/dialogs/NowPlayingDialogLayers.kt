package com.example.onemusic.ui.screens.player.dialogs

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.components.ApexEqualizerDialog
import com.example.onemusic.ui.components.ApexTrackActionSheet
import com.example.onemusic.ui.components.TrackDetailsDialog
import dev.chrisbanes.haze.HazeState

/** Hộp thoại / bảng đang mở trên Now Playing (giữ bằng `remember` ở NowPlayingSheet). */
@Stable
internal class NowPlayingDialogsState {
    var isSpeedMenuOpen by mutableStateOf(false)
    var showSleepTimerDialog by mutableStateOf(false)
    var showTrackDetailsDialog by mutableStateOf(false)
    var showEqualizerDialog by mutableStateOf(false)
    var showOptionsMenu by mutableStateOf(false)
}

/**
 * Các lớp hộp thoại phủ lên Now Playing: tốc độ phát, hẹn giờ ngủ, thông tin bài (theo trang pager đang xem),
 * EQ, bảng thao tác bài (theo bài đang hiển thị). Mở/đóng qua [dialogs].
 */
@Composable
internal fun NowPlayingDialogLayers(
    dialogs: NowPlayingDialogsState,
    playbackState: PlaybackState,
    queue: List<Track>,
    pagerState: PagerState,
    track: Track?,
    displayedTrack: Track?,
    audioEffectManager: AudioEffectManager?,
    motionVideoPath: String?,
    hazeState: HazeState,
    onSetPlaybackSpeed: ((Float) -> Unit)?,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleFavorite: (trackId: String, isCurrentlyFavorite: Boolean) -> Unit,
    onAddToPlaylist: ((Track) -> Unit)?,
    onRemoveQueueItem: ((Int) -> Unit)?,
    onRemoveMotionArtwork: (() -> Unit)?
) {
    // LAYER 3: Playback Speed Selection Dialog
    if (dialogs.isSpeedMenuOpen) {
        PlaybackSpeedDialog(
            currentSpeed = playbackState.playbackSpeed,
            hazeState = hazeState,
            onSelectSpeed = { speed -> onSetPlaybackSpeed?.invoke(speed) },
            onDismiss = { dialogs.isSpeedMenuOpen = false }
        )
    }

    // LAYER 4: Sleep Timer Dialog
    if (dialogs.showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerMinutes = playbackState.sleepTimerMinutes,
            sleepTimerRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
            hazeState = hazeState,
            onSetSleepTimer = onSetSleepTimer,
            onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
            onCancelSleepTimer = onCancelSleepTimer,
            onDismiss = { dialogs.showSleepTimerDialog = false }
        )
    }

    // LAYER 5: Track Audio Inspector Dialog
    val currentInspectedTrack = if (queue.isNotEmpty() && pagerState.currentPage in queue.indices) queue[pagerState.currentPage] else track
    if (dialogs.showTrackDetailsDialog && currentInspectedTrack != null) {
        TrackDetailsDialog(
            track = currentInspectedTrack,
            hazeState = hazeState,
            onDismiss = { dialogs.showTrackDetailsDialog = false }
        )
    }

    // LAYER 6: Equalizer & SoundAlive DSP Dialog
    if (dialogs.showEqualizerDialog && audioEffectManager != null) {
        ApexEqualizerDialog(
            audioEffectManager = audioEffectManager,
            hazeState = hazeState,
            onDismiss = { dialogs.showEqualizerDialog = false }
        )
    }

    // LAYER 7: OneMusic Apex Prism Track Action Sheet (1:1 Apple Music Modal Bottom Sheet)
    val actionSheetTrack = displayedTrack ?: track
    if (dialogs.showOptionsMenu && actionSheetTrack != null) {
        ApexTrackActionSheet(
            track = actionSheetTrack,
            onDismissRequest = { dialogs.showOptionsMenu = false },
            onToggleFavorite = { trackId ->
                val isCurrentlyFav = actionSheetTrack.isFavorite
                onToggleFavorite(trackId, isCurrentlyFav)
            },
            onAddToPlaylist = onAddToPlaylist,
            onDeleteTrack = { trk ->
                // FIX: Trước đây luôn xóa theo playbackState.currentIndex (bài đang phát),
                // bỏ qua tham số trk (bài thực sự đang hiển thị trong Action Sheet, có thể
                // khác bài đang phát khi người dùng vừa vuốt xem trước bài kế/trước).
                // Ưu tiên dùng pagerState.currentPage (vị trí đang xem trước trong pager) vì
                // đó là chỉ số chính xác; chỉ dùng indexOfFirst theo id làm phương án dự phòng
                // khi hàng đợi có 2 bài trùng track.id và pager không khớp trk.
                val targetIndex = if (pagerState.currentPage in playbackState.queue.indices &&
                    playbackState.queue[pagerState.currentPage].id == trk.id) {
                    pagerState.currentPage
                } else {
                    playbackState.queue.indexOfFirst { it.id == trk.id }
                }
                if (targetIndex != -1) {
                    onRemoveQueueItem?.invoke(targetIndex)
                } else {
                    onRemoveQueueItem?.invoke(playbackState.currentIndex)
                }
            },
            onOpenCredits = { dialogs.showTrackDetailsDialog = true },
            onOpenSleepTimer = { dialogs.showSleepTimerDialog = true },
            hasMotionArtwork = (motionVideoPath != null && track?.id == actionSheetTrack.id),
            onRemoveMotionArtwork = onRemoveMotionArtwork,
            hazeState = hazeState
        )
    }
}
