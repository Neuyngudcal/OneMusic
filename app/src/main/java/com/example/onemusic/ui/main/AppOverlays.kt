package com.example.onemusic.ui.main

import androidx.compose.animation.AnimatedVisibility
import com.example.onemusic.theme.NowPlayingThemeScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.ui.components.AddToPlaylistDialog
import com.example.onemusic.ui.navigation.Screen
import com.example.onemusic.ui.screens.dedup.DuplicateCleanerScreen
import com.example.onemusic.ui.screens.folder.FolderManagerScreen
import com.example.onemusic.ui.screens.player.NowPlayingSheet
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.playback.PlaybackState

/** Các lớp phủ trên tab: Thư mục nhạc, Dọn trùng lặp, Now Playing toàn màn hình, hộp thoại thêm vào playlist. */
@Composable
internal fun AppOverlays(
    musicRepository: MusicRepository,
    playerController: MusicPlayerController,
    isFolderManagerVisible: Boolean,
    onCloseFolderManager: () -> Unit,
    isDuplicateCleanerVisible: Boolean,
    onCloseDuplicateCleaner: () -> Unit,
    isPlayerExpanded: Boolean,
    onCollapsePlayer: () -> Unit,
    playbackState: PlaybackState,
    appSettings: AppSettings,
    motionVideoPath: String?,
    trackToAddToPlaylist: Track?,
    customPlaylists: List<CustomPlaylist>,
    onOpenAddToPlaylist: (Track) -> Unit,
    onDismissAddToPlaylist: () -> Unit
) {
    // Folder Manager Screen Overlay (One UI 8.5 Style - Gentle, Luxurious Spring Slide)
    AnimatedVisibility(
        visible = isFolderManagerVisible,
        enter = slideInVertically(
            initialOffsetY = { (it * 0.35f).toInt() },
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 280f)
        ) + fadeIn(tween(320, easing = FastOutSlowInEasing)),
        exit = slideOutVertically(
            targetOffsetY = { (it * 0.35f).toInt() },
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 300f)
        ) + fadeOut(tween(260, easing = FastOutSlowInEasing)),
        modifier = Modifier.fillMaxSize()
    ) {
        FolderManagerScreen(
            musicRepository = musicRepository,
            onBack = onCloseFolderManager
        )
    }

    // Duplicate Cleaner Screen Overlay (One UI 8.5 Style - Gentle Spring Slide)
    AnimatedVisibility(
        visible = isDuplicateCleanerVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 320f)
        ) + fadeIn(tween(260, easing = FastOutSlowInEasing)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 340f)
        ) + fadeOut(tween(200, easing = FastOutSlowInEasing)),
        modifier = Modifier.fillMaxSize()
    ) {
        DuplicateCleanerScreen(
            musicRepository = musicRepository,
            playerController = playerController,
            onBack = onCloseDuplicateCleaner
        )
    }

    // Fullscreen Expanded Now Playing Player Sheet
    // Chỉ cho video bìa động chạy khi sheet đang mở
    LaunchedEffect(isPlayerExpanded) {
        playerController.setMotionPlaybackAllowed(isPlayerExpanded)
    }
    if (isPlayerExpanded) {
        NowPlayingThemeScope {
            NowPlayingSheet(
                playbackState = playbackState,
                // Truyền cả object State (không đọc .value ở đây) để màn gốc không vẽ lại mỗi 40ms
                positionState = playerController.positionMs.collectAsState(),
                onCollapse = onCollapsePlayer,
                onPlayPause = { playerController.togglePlayPause() },
                onNext = { playerController.skipToNext() },
                onPrevious = { playerController.skipToPrevious() },
                onSeek = { pos -> playerController.seekTo(pos) },
                onToggleShuffle = { playerController.toggleShuffle() },
                onCycleRepeat = { playerController.cycleRepeatMode() },
                onToggleAutoplay = { playerController.toggleAutoplay() },
                onClearPlaybackHistory = { playerController.clearPlaybackHistory() },
                onToggleFavorite = { trackId ->
                    val isFav = musicRepository.toggleFavorite(trackId)
                    playerController.updateTrackFavorite(trackId, isFav)
                },
                onPlayQueueIndex = { index -> playerController.playQueueIndex(index) },
                onMoveQueueItem = { from, to -> playerController.moveQueueItem(from, to) },
                onRemoveQueueItem = { index -> playerController.removeQueueItem(index) },
                onSetPlaybackSpeed = { speed -> playerController.setPlaybackSpeed(speed) },
                onSetSleepTimer = { minutes -> playerController.setSleepTimer(minutes) },
                onSetSleepTimerEndOfTrack = { playerController.setSleepTimerEndOfTrack() },
                onCancelSleepTimer = { playerController.cancelSleepTimer() },
                onAddToPlaylist = onOpenAddToPlaylist,
                audioEffectManager = playerController.audioEffectManager,
                audioOutputManager = playerController.audioOutputManager,
                appSettings = appSettings,
                motionVideoPath = motionVideoPath,
                motionPlayer = playerController.motionExoPlayer,
                onRemoveMotionArtwork = { playerController.removeMotionArtworkForCurrentTrack() }
            )
        }
    }

    // Add to Playlist Dialog (mở từ trong Now Playing thì giữ giao diện tối của Now Playing)
    trackToAddToPlaylist?.let { track ->
        val addToPlaylistDialog: @Composable () -> Unit = {
            AddToPlaylistDialog(
                track = track,
                playlists = customPlaylists,
                onCreatePlaylist = { name -> musicRepository.createPlaylist(name) },
                onAddToPlaylist = { playlistId, trackId ->
                    musicRepository.addTrackToPlaylist(playlistId, trackId)
                },
                onDismiss = onDismissAddToPlaylist
            )
        }
        if (isPlayerExpanded) {
            NowPlayingThemeScope(addToPlaylistDialog)
        } else {
            addToPlaylistDialog()
        }
    }
}
