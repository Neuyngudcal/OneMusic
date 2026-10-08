package com.example.onemusic.ui.main

import androidx.compose.runtime.remember
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.ui.components.NowBar
import com.example.onemusic.ui.navigation.Screen
import com.example.onemusic.ui.screens.home.HomeScreen
import com.example.onemusic.ui.screens.library.LibraryScreen
import com.example.onemusic.ui.screens.search.SearchScreen
import com.example.onemusic.ui.screens.settings.SettingsScreen
import com.example.onemusic.data.local.CustomPlaylist
import androidx.compose.runtime.saveable.SaveableStateHolder

/**
 * Nội dung 4 tab chính (Trang chủ, Thư viện, Tìm kiếm, Cài đặt) với hiệu ứng trượt khi đổi tab.
 * Mỗi tab được bọc [SaveableStateHolder] để giữ vị trí cuộn / rememberSaveable khi chuyển tab.
 */
@Composable
internal fun MainTabsHost(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    tabStateHolder: SaveableStateHolder,
    hazeState: HazeState,
    tracks: List<Track>,
    currentTrackId: String?,
    customPlaylists: List<CustomPlaylist>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    settingsPreferences: SettingsPreferences,
    musicRepository: MusicRepository,
    playerController: MusicPlayerController,
    m3uLaunchers: PlaylistM3uLaunchers,
    playFromContext: (Track, List<Track>) -> Unit,
    onOpenAddToPlaylist: (Track) -> Unit,
    onOpenFolders: () -> Unit,
    onOpenDuplicateCleaner: () -> Unit,
    onRescan: () -> Unit
) {
    val tabSlideSpec = remember {
        tween<androidx.compose.ui.unit.IntOffset>(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    }

    AnimatedContent(
        targetState = currentScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val isForward = targetState.ordinal > initialState.ordinal
            val enter = slideInHorizontally(
                initialOffsetX = { if (isForward) (it * 0.08f).toInt() else (-it * 0.08f).toInt() },
                animationSpec = tabSlideSpec
            ) + fadeIn(
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            )

            val exit = slideOutHorizontally(
                targetOffsetX = { if (isForward) (-it * 0.08f).toInt() else (it * 0.08f).toInt() },
                animationSpec = tabSlideSpec
            ) + fadeOut(
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )

            (enter togetherWith exit).apply {
                targetContentZIndex = 1f
            }
        },
        label = "main_screen_tab_transition"
    ) { screen ->
        // Giữ state (rememberSaveable, vị trí cuộn...) của từng tab khi tab bị gỡ khỏi màn hình lúc chuyển tab
        tabStateHolder.SaveableStateProvider(screen.name) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState, key = screen)
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    tracks = tracks,
                    currentTrackId = currentTrackId,
                    customPlaylists = customPlaylists,
                    onTrackSelect = playFromContext,
                    // Mục 8: menu thao tác từng bài
                    onOpenAddToPlaylist = onOpenAddToPlaylist,
                    onAddToQueue = { track -> playerController.addToQueue(track) },
                    onShuffleAll = {
                        playerController.setQueue(tracks.shuffled(), startIndex = 0, autoPlay = true)
                    },
                    onToggleFavorite = { trackId ->
                        val isFav = musicRepository.toggleFavorite(trackId)
                        playerController.updateTrackFavorite(trackId, isFav)
                    },
                    onCreatePlaylist = { name ->
                        musicRepository.createPlaylist(name)
                    },
                    onRenamePlaylist = { id, name ->
                        musicRepository.renamePlaylist(id, name)
                    },
                    onDeletePlaylist = { id ->
                        musicRepository.deletePlaylist(id)
                    },
                    onRemoveTrackFromPlaylist = { playlistId, trackId ->
                        musicRepository.removeTrackFromPlaylist(playlistId, trackId)
                    },
                    onAddTrackToPlaylist = { playlistId, trackId ->
                        musicRepository.addTrackToPlaylist(playlistId, trackId)
                    },
                    onPlayTracks = { trackList ->
                        playerController.setQueue(trackList, startIndex = 0, autoPlay = true)
                    },
                    onPlayNext = { track ->
                        playerController.playNext(track)
                    },
                    onExportPlaylistM3u = m3uLaunchers.exportPlaylist,
                    onImportPlaylistM3u = m3uLaunchers.importPlaylist,
                    onOpenFolders = onOpenFolders,
                    onOpenSettings = { onNavigate(Screen.SETTINGS) },
                    onRescan = onRescan
                )
                Screen.LIBRARY -> LibraryScreen(
                    tracks = tracks,
                    currentTrackId = currentTrackId,
                    customPlaylists = customPlaylists,
                    onCreatePlaylist = { name ->
                        musicRepository.createPlaylist(name)
                    },
                    onAddToPlaylist = { playlistId, trackId ->
                        musicRepository.addTrackToPlaylist(playlistId, trackId)
                    },
                    onPlayTracks = { trackList ->
                        playerController.setQueue(trackList, startIndex = 0, autoPlay = true)
                    },
                    onPlayNext = { track ->
                        playerController.playNext(track)
                    },
                    onPlayNextTracks = { selectedList ->
                        playerController.playNextTracks(selectedList)
                    },
                    onImportPlaylistM3u = m3uLaunchers.importPlaylist,
                    onTrackSelect = playFromContext,
                    // Mục 8: menu thao tác từng bài
                    onOpenAddToPlaylist = onOpenAddToPlaylist,
                    onAddToQueue = { track -> playerController.addToQueue(track) },
                    onShuffleAll = {
                        playerController.setQueue(tracks.shuffled(), startIndex = 0, autoPlay = true)
                    },
                    onToggleFavorite = { trackId ->
                        val isFav = musicRepository.toggleFavorite(trackId)
                        playerController.updateTrackFavorite(trackId, isFav)
                    },
                    onRescan = onRescan,
                    onOpenFolders = onOpenFolders,
                    onOpenSettings = { onNavigate(Screen.SETTINGS) }
                )
                Screen.SEARCH -> SearchScreen(
                    tracks = tracks,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    // Mục 7b: bấm bài chỉ phát, ở lại màn Tìm kiếm (giống Home/Thư viện); muốn mở thì bấm NowBar
                    onTrackSelect = playFromContext,
                    // Mục 8: menu thao tác từng bài
                    onOpenAddToPlaylist = onOpenAddToPlaylist,
                    onAddToQueue = { track -> playerController.addToQueue(track) },
                    onPlayNext = { track -> playerController.playNext(track) },
                    onPlayTracks = { trackList ->
                        playerController.setQueue(trackList, startIndex = 0, autoPlay = true)
                    },
                    onToggleFavorite = { trackId ->
                        val isFav = musicRepository.toggleFavorite(trackId)
                        playerController.updateTrackFavorite(trackId, isFav)
                    }
                )
                Screen.SETTINGS -> SettingsScreen(
                    settingsPreferences = settingsPreferences,
                    onBack = { onNavigate(Screen.HOME) },
                    audioEffectManager = playerController.audioEffectManager,
                    onOpenFolders = onOpenFolders,
                    onOpenDuplicateCleaner = onOpenDuplicateCleaner,
                    onRescan = onRescan,
                    onClearMotionCache = { playerController.clearMotionArtworkCache() }
                )
            }
        }
        }
    }
}
