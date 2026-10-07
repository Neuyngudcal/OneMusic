package com.example.onemusic

import com.example.onemusic.haptics.LocalApexHaptics
import com.example.onemusic.haptics.rememberApexHaptics
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeState
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.ui.components.AddToPlaylistDialog
import com.example.onemusic.ui.components.NowBar
import com.example.onemusic.ui.navigation.ApexBottomNavigation
import com.example.onemusic.ui.navigation.Screen
import com.example.onemusic.ui.screens.dedup.DuplicateCleanerScreen
import com.example.onemusic.ui.screens.folder.FolderManagerScreen
import com.example.onemusic.ui.screens.home.HomeScreen
import com.example.onemusic.ui.screens.library.LibraryScreen
import com.example.onemusic.ui.screens.player.NowPlayingSheet
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.onemusic.ui.screens.search.SearchScreen
import com.example.onemusic.ui.screens.settings.SettingsScreen

import android.net.Uri
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.playlist.M3uPlaylistManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import com.example.onemusic.ui.utils.LocalAppSnackbar
import com.example.onemusic.ui.utils.LocalBottomOverlayPadding
import com.example.onemusic.ui.utils.ShowSnackbar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.OneMusicTheme
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private lateinit var musicRepository: MusicRepository
    private lateinit var audioEffectManager: AudioEffectManager
    private lateinit var playerController: MusicPlayerController
    private val externalPlayTrigger = mutableStateOf(0L)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission result handled
    }

    private val storageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            context?.let {
                musicRepository.refreshStorageAvailability(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android Core SplashScreen with instant dismissal
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Initialize repository and fast controller instances
        musicRepository = MusicRepository.getInstance(applicationContext)
        playerController = MusicPlayerController.getInstance(applicationContext)
        audioEffectManager = playerController.audioEffectManager

        // Handle possible external audio VIEW intent on start
        handleAudioIntent(intent)

        // 3. Direct UI render with true zero artificial delay (instant 120Hz display)
        setContent {
            OneMusicTheme {
                LaunchedEffect(Unit) {
                    requestNotificationPermission()
                }
                OneMusicApp(
                    musicRepository = musicRepository,
                    playerController = playerController,
                    externalPlayTrigger = externalPlayTrigger.value
                )
            }
        }

        // 4. Asynchronously register storage receivers with RECEIVER_NOT_EXPORTED
        lifecycleScope.launch(Dispatchers.IO) {
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(android.content.Intent.ACTION_MEDIA_EJECT)
                addAction(android.content.Intent.ACTION_MEDIA_REMOVED)
                addAction(android.content.Intent.ACTION_MEDIA_BAD_REMOVAL)
                addAction(android.content.Intent.ACTION_MEDIA_MOUNTED)
                addDataScheme("file")
            }
            try {
                ContextCompat.registerReceiver(
                    this@MainActivity,
                    storageReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (_: Throwable) {}
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAudioIntent(intent)
    }

    private fun handleAudioIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            lifecycleScope.launch(Dispatchers.IO) {
                val track = extractTrackFromUri(uri)
                if (track != null) {
                    withContext(Dispatchers.Main) {
                        playerController.setQueue(listOf(track), startIndex = 0, autoPlay = true)
                        externalPlayTrigger.value = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun extractTrackFromUri(uri: Uri): Track? {
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(applicationContext, uri)
            } catch (_: Exception) {
                return null
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.ifBlank { null }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "External Audio"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.ifBlank { null } ?: "Unknown Artist"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()

            Track(
                id = "ext_${System.currentTimeMillis()}_${uri.hashCode()}",
                title = title,
                artist = artist,
                album = "Tệp ngoài",
                durationMs = durationMs,
                audioUrl = uri.toString(),
                artworkUrl = "",
                isFavorite = false
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        musicRepository.refreshStorageAvailability(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(storageReceiver)
        } catch (_: Exception) {}
    }
}

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
    var playlistToExport by remember { mutableStateOf<CustomPlaylist?>(null) }

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

    // M3U8 Export Launcher (SAF CreateDocument)
    val exportM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null && playlistToExport != null) {
            val pl = playlistToExport ?: return@rememberLauncherForActivityResult
            scope.launch {
                val content = M3uPlaylistManager.exportPlaylistToM3uString(pl, tracks)
                val success = M3uPlaylistManager.writeContentToUri(context, uri, content)
                if (success) {
                    showSnackbar("Đã xuất playlist \"${pl.name}\"", null, null)
                } else {
                    showSnackbar("Không xuất được playlist. Hãy thử chọn vị trí lưu khác.", null, null)
                }
                playlistToExport = null
            }
        }
    }

    // M3U8 Import Launcher (SAF OpenDocument)
    val importM3uLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = M3uPlaylistManager.importPlaylistFromUri(context, uri, tracks)
                if (result != null) {
                    val (name, trackIds) = result
                    val newPl = musicRepository.createPlaylist(name)
                    if (newPl != null) {
                        for (tid in trackIds) {
                            musicRepository.addTrackToPlaylist(newPl.id, tid)
                        }
                        showSnackbar("Đã nhập playlist \"$name\" (${trackIds.size} bài hát)", null, null)
                    }
                } else {
                    showSnackbar("Không đọc được tệp playlist. Hãy chọn tệp .m3u hoặc .m3u8 khác.", null, null)
                }
            }
        }
    }

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
        // Main Screen Content Container (Home, Library, Search) with Samsung One UI 8.5 Shared Surface Glide
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
                        currentTrackId = playbackState.currentTrack?.id,
                        customPlaylists = customPlaylists,
                        onTrackSelect = playFromContext,
                        // Mục 8: menu thao tác từng bài
                        onOpenAddToPlaylist = { track -> trackToAddToPlaylist = track },
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
                        onExportPlaylistM3u = { pl ->
                            playlistToExport = pl
                            exportM3uLauncher.launch("${pl.name}.m3u8")
                        },
                        onImportPlaylistM3u = {
                            importM3uLauncher.launch(arrayOf("audio/*", "application/x-mpegurl", "audio/x-mpegurl", "text/plain", "*/*"))
                        },
                        onOpenFolders = {
                            isFolderManagerVisible = true
                        },
                        onOpenSettings = {
                            currentScreen = Screen.SETTINGS
                        },
                        onRescan = rescanLibrary
                    )
                    Screen.LIBRARY -> LibraryScreen(
                        tracks = tracks,
                        currentTrackId = playbackState.currentTrack?.id,
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
                        onImportPlaylistM3u = {
                            importM3uLauncher.launch(arrayOf("audio/*", "application/x-mpegurl", "audio/x-mpegurl", "text/plain", "*/*"))
                        },
                        onTrackSelect = playFromContext,
                        // Mục 8: menu thao tác từng bài
                        onOpenAddToPlaylist = { track -> trackToAddToPlaylist = track },
                        onAddToQueue = { track -> playerController.addToQueue(track) },
                        onShuffleAll = {
                            playerController.setQueue(tracks.shuffled(), startIndex = 0, autoPlay = true)
                        },
                        onToggleFavorite = { trackId ->
                            val isFav = musicRepository.toggleFavorite(trackId)
                            playerController.updateTrackFavorite(trackId, isFav)
                        },
                        onRescan = rescanLibrary,
                        onOpenFolders = {
                            isFolderManagerVisible = true
                        },
                        onOpenSettings = {
                            currentScreen = Screen.SETTINGS
                        }
                    )
                    Screen.SEARCH -> SearchScreen(
                        tracks = tracks,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        // Mục 7b: bấm bài chỉ phát, ở lại màn Tìm kiếm (giống Home/Thư viện); muốn mở thì bấm NowBar
                        onTrackSelect = playFromContext,
                        // Mục 8: menu thao tác từng bài
                        onOpenAddToPlaylist = { track -> trackToAddToPlaylist = track },
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
                        onBack = { currentScreen = Screen.HOME },
                        audioEffectManager = playerController.audioEffectManager,
                        onOpenFolders = { isFolderManagerVisible = true },
                        onOpenDuplicateCleaner = { isDuplicateCleanerVisible = true },
                        onRescan = rescanLibrary,
                        onClearMotionCache = { playerController.clearMotionArtworkCache() }
                    )
                }
            }
            }
        }

        // Floating Bottom Controls: NowBar & Bottom Navigation Dock (Automatically hides when keyboard is open)
        val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

        AnimatedVisibility(
            visible = !isFolderManagerVisible && !isDuplicateCleanerVisible && !isKeyboardOpen,
            enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
            ),
            exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (it.height > 0) bottomOverlayHeightPx = it.height }
            ) {
                // Chỉ báo quét nhạc: không chặn thao tác, hiện ở mọi tab
                AnimatedVisibility(
                    visible = isScanning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(PillShape)
                            .background(SurfaceElevated.copy(alpha = 0.92f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Brand, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Đang quét thư viện nhạc…", color = TextPrimary, fontSize = 13.sp)
                    }
                }

                // Floating Mini Player (Now Bar)
                AnimatedVisibility(
                    visible = playbackState.currentTrack != null,
                    enter = fadeIn(tween(180, easing = FastOutSlowInEasing)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + shrinkVertically(tween(200))
                ) {
                    NowBar(
                        track = playbackState.currentTrack,
                        isPlaying = playbackState.isPlaying,
                        onBarClick = {
                            if (playbackState.currentTrack != null) {
                                isPlayerExpanded = true
                            } else if (tracks.isNotEmpty()) {
                                playerController.setQueue(tracks, startIndex = 0, autoPlay = false)
                                isPlayerExpanded = true
                            }
                        },
                        onPlayPauseClick = {
                            if (playbackState.currentTrack != null) {
                                playerController.togglePlayPause()
                            } else if (tracks.isNotEmpty()) {
                                playerController.setQueue(tracks, startIndex = 0, autoPlay = true)
                            }
                        },
                        onNextClick = { playerController.skipToNext() },
                        onPreviousClick = { playerController.skipToPrevious() }
                    )
                }

                // Floating Bottom Navigation Dock (4 Tabs Icon-Only)
                ApexBottomNavigation(
                    currentScreen = currentScreen,
                    onScreenSelected = { currentScreen = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }
        }

        // Snackbar dùng chung: nằm ngay trên cụm NowBar + thanh tab để không bị che
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomOverlayPadding)
        ) { data ->
            androidx.compose.material3.Snackbar(
                snackbarData = data,
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                containerColor = com.example.onemusic.theme.SurfaceElevated,
                contentColor = TextPrimary,
                actionColor = com.example.onemusic.theme.Brand
            )
        }

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
                onBack = { isFolderManagerVisible = false }
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
                onBack = { isDuplicateCleanerVisible = false }
            )
        }

        // Fullscreen Expanded Now Playing Player Sheet
        // Chỉ cho video bìa động chạy khi sheet đang mở
        LaunchedEffect(isPlayerExpanded) {
            playerController.setMotionPlaybackAllowed(isPlayerExpanded)
        }
        if (isPlayerExpanded) {
            NowPlayingSheet(
                playbackState = playbackState,
                // Truyền cả object State (không đọc .value ở đây) để MainActivity không vẽ lại mỗi 40ms
                positionState = playerController.positionMs.collectAsState(),
                onCollapse = { isPlayerExpanded = false },
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
                onAddToPlaylist = { track -> trackToAddToPlaylist = track },
                audioEffectManager = playerController.audioEffectManager,
                audioOutputManager = playerController.audioOutputManager,
                appSettings = appSettings,
                motionVideoPath = motionVideoPath,
                motionPlayer = playerController.motionExoPlayer,
                onRemoveMotionArtwork = { playerController.removeMotionArtworkForCurrentTrack() }
            )
        }

        // Add to Playlist Dialog
        trackToAddToPlaylist?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = customPlaylists,
                onCreatePlaylist = { name -> musicRepository.createPlaylist(name) },
                onAddToPlaylist = { playlistId, trackId ->
                    musicRepository.addTrackToPlaylist(playlistId, trackId)
                },
                onDismiss = { trackToAddToPlaylist = null }
            )
        }
    }
    }
}

