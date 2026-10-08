package com.example.onemusic.ui.main

import com.example.onemusic.haptics.LocalApexHaptics
import com.example.onemusic.haptics.rememberApexHaptics
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.ui.components.NowBar
import com.example.onemusic.ui.navigation.Screen
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.onemusic.ui.utils.LocalAppSnackbar
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.ui.utils.ShowSnackbar
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import androidx.compose.material3.SnackbarHostState

/**
 * Composable gốc của app: giữ state điều hướng (tab, Now Playing, các lớp phủ), Snackbar dùng chung,
 * BackHandler, ẩn/hiện thanh hệ thống; ghép tab chính + cụm điều khiển đáy + các lớp phủ.
 */
@Composable
fun OneMusicApp(
    musicRepository: MusicRepository,
    playerController: MusicPlayerController,
    externalPlayTrigger: Long = 0L
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsPreferences = remember { SettingsPreferences(context) }
    val appSettings by settingsPreferences.settingsFlow.collectAsState()

    val tracks by musicRepository.tracks.collectAsState(initial = emptyList())
    val isScanning by musicRepository.isScanning.collectAsState()
    val customPlaylists by musicRepository.playlists.collectAsState(initial = emptyList())
    val playbackState by playerController.playbackState.collectAsState()
    val motionVideoPath by playerController.motionVideoPath.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isFolderManagerVisible by remember { mutableStateOf(false) }
    var isDuplicateCleanerVisible by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }

    // Phát bài được bấm trong ĐÚNG danh sách người dùng đang xem (album, playlist, kết quả tìm kiếm...),
    // thay vì luôn lấy cả thư viện làm hàng đợi
    val playFromContext: (Track, List<Track>) -> Unit = { track, contextTracks ->
        val list = contextTracks.ifEmpty { tracks }
        val idx = list.indexOf(track).coerceAtLeast(0)
        playerController.setQueue(list, startIndex = idx, autoPlay = true)
    }

    // Snackbar dùng chung (thay Toast): hiện trong giao diện app, có thể có nút "Hoàn tác"
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val showSnackbar: ShowSnackbar = remember(snackbarHostState) {
        { message, actionLabel, onAction ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) onAction?.invoke()
            }
        }
    }

    // Respond to external audio VIEW intent
    LaunchedEffect(externalPlayTrigger) {
        if (externalPlayTrigger > 0L) {
            isPlayerExpanded = true
        }
    }

    // Pre-populate queue on startup or restore last playback state + launch background motion artwork scan
    LaunchedEffect(tracks, appSettings.isMotionArtworkEnabled, appSettings.isAutoMotionScanEnabled) {
        if (tracks.isNotEmpty()) {
            if (appSettings.isMotionArtworkEnabled && appSettings.isAutoMotionScanEnabled) {
                musicRepository.startBackgroundMotionScan(tracks)
            } else {
                musicRepository.cancelBackgroundMotionScan()
            }
            if (playerController.playbackState.value.queue.isEmpty()) {
                playerController.restoreLastPlaybackState(tracks)
                if (playerController.playbackState.value.queue.isEmpty()) {
                    playerController.setQueue(tracks, startIndex = 0, autoPlay = false)
                }
            }
        }
    }

    // Xuất/nhập playlist .m3u8 (xem PlaylistM3uLaunchers.kt)
    val m3uLaunchers = rememberPlaylistM3uLaunchers(tracks, musicRepository, showSnackbar)

    // Dynamic System Bars control (Status bar / Navigation bar hiding)
    val activity = context as? androidx.activity.ComponentActivity
    LaunchedEffect(appSettings.isHideStatusBarEnabled, appSettings.isHideNavigationBarEnabled) {
        activity?.window?.let { window ->
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (appSettings.isHideStatusBarEnabled) {
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }

            if (appSettings.isHideNavigationBarEnabled) {
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            } else {
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Intercept back gesture gracefully
    BackHandler(enabled = isDuplicateCleanerVisible || isFolderManagerVisible || isPlayerExpanded || trackToAddToPlaylist != null || currentScreen != Screen.HOME) {
        when {
            trackToAddToPlaylist != null -> trackToAddToPlaylist = null
            isDuplicateCleanerVisible -> isDuplicateCleanerVisible = false
            isFolderManagerVisible -> isFolderManagerVisible = false
            isPlayerExpanded -> isPlayerExpanded = false
            currentScreen == Screen.SEARCH -> {
                searchQuery = ""
                currentScreen = Screen.HOME
            }
            currentScreen != Screen.HOME -> currentScreen = Screen.HOME
        }
    }

    val hazeState = remember { HazeState() }
    val hapticEngine = rememberApexHaptics()
    val tabStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    // Đo chiều cao thật của cụm NowBar + thanh tab nổi → khoảng đệm dưới cho mọi danh sách
    val density = androidx.compose.ui.platform.LocalDensity.current
    var bottomOverlayHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val bottomOverlayPadding = with(density) { bottomOverlayHeightPx.toDp() } + 16.dp

    // Quét lại toàn thư viện (dùng chung cho Home, Thư viện, Cài đặt). Trong lúc quét, pill
    // "Đang quét thư viện nhạc…" trên NowBar báo tiến trình; xong thì báo kết quả bằng Snackbar.
    val rescanLibrary: () -> Unit = {
        if (musicRepository.folders.value.isEmpty()) {
            isFolderManagerVisible = true
        } else {
            musicRepository.rescanAllMusic(context) { count ->
                showSnackbar(
                    if (count > 0) "Đã quét xong: $count bài hát" else "Không tìm thấy bài hát nào trong các thư mục đã chọn",
                    null,
                    null
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalApexHazeState provides hazeState,
        LocalApexHaptics provides hapticEngine,
        LocalBottomOverlayPadding provides bottomOverlayPadding,
        LocalAppSnackbar provides showSnackbar
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
        ) {
        MainTabsHost(
            currentScreen = currentScreen,
            onNavigate = { currentScreen = it },
            tabStateHolder = tabStateHolder,
            hazeState = hazeState,
            tracks = tracks,
            currentTrackId = playbackState.currentTrack?.id,
            customPlaylists = customPlaylists,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            settingsPreferences = settingsPreferences,
            musicRepository = musicRepository,
            playerController = playerController,
            m3uLaunchers = m3uLaunchers,
            playFromContext = playFromContext,
            onOpenAddToPlaylist = { track -> trackToAddToPlaylist = track },
            onOpenFolders = { isFolderManagerVisible = true },
            onOpenDuplicateCleaner = { isDuplicateCleanerVisible = true },
            onRescan = rescanLibrary
        )

        // Floating Bottom Controls: NowBar & Bottom Navigation Dock (Automatically hides when keyboard is open)
        BottomControlsOverlay(
            isFullScreenOverlayVisible = isFolderManagerVisible || isDuplicateCleanerVisible,
            isScanning = isScanning,
            currentTrack = playbackState.currentTrack,
            isPlaying = playbackState.isPlaying,
            tracks = tracks,
            playerController = playerController,
            onExpandPlayer = { isPlayerExpanded = true },
            currentScreen = currentScreen,
            onNavigate = { currentScreen = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onHeightMeasured = { bottomOverlayHeightPx = it },
            snackbarHostState = snackbarHostState,
            bottomOverlayPadding = bottomOverlayPadding
        )

        AppOverlays(
            musicRepository = musicRepository,
            playerController = playerController,
            isFolderManagerVisible = isFolderManagerVisible,
            onCloseFolderManager = { isFolderManagerVisible = false },
            isDuplicateCleanerVisible = isDuplicateCleanerVisible,
            onCloseDuplicateCleaner = { isDuplicateCleanerVisible = false },
            isPlayerExpanded = isPlayerExpanded,
            onCollapsePlayer = { isPlayerExpanded = false },
            playbackState = playbackState,
            appSettings = appSettings,
            motionVideoPath = motionVideoPath,
            trackToAddToPlaylist = trackToAddToPlaylist,
            customPlaylists = customPlaylists,
            onOpenAddToPlaylist = { track -> trackToAddToPlaylist = track },
            onDismissAddToPlaylist = { trackToAddToPlaylist = null }
        )
    }
    }
}
