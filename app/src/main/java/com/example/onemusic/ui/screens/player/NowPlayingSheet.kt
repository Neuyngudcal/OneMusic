package com.example.onemusic.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.AudioOutputManager
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.screens.player.backdrop.NowPlayingBackdrop
import com.example.onemusic.ui.screens.player.controls.NowPlayingControlsDeck
import com.example.onemusic.ui.screens.player.dialogs.FavoriteToastBanner
import com.example.onemusic.ui.screens.player.dialogs.NowPlayingDialogLayers
import com.example.onemusic.ui.screens.player.dialogs.NowPlayingDialogsState
import com.example.onemusic.ui.screens.player.state.rememberArtworkPagerState
import com.example.onemusic.ui.screens.player.state.rememberCenterViewAutoScroll
import com.example.onemusic.ui.screens.player.state.rememberControlsDeckVisibility
import com.example.onemusic.ui.screens.player.state.rememberNowPlayingSheetState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch





/**
 * Modern Fullscreen Music Player (Now Playing Sheet) - ONE PAGE ARCHITECTURE
 * - 100% Full-Bleed Edge-to-Edge Hero Cover & Motion Video (Permanently Mounted & CenterCrop Precision)
 * - Frosted Glass ("Nhám mờ") Backdrop Transition for Lyrics & Queue
 * - Collapsible Playback Controls Deck on Lyrics Scroll (Auto-hide on scroll down, Auto-reveal on scroll top)
 * - Clean Top Drag Handle [ — ] with Zero Clutter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    // Vị trí phát (cập nhật mỗi 40ms). Chỉ đọc .value ở composable con cần nó để tránh vẽ lại cả sheet.
    positionState: androidx.compose.runtime.State<Long>,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: (() -> Unit)? = null,
    onClearPlaybackHistory: (() -> Unit)? = null,
    onToggleFavorite: (String) -> Unit,
    onPlayQueueIndex: (Int) -> Unit = {},
    onMoveQueueItem: ((Int, Int) -> Unit)? = null,
    onRemoveQueueItem: ((Int) -> Unit)? = null,
    onSetPlaybackSpeed: ((Float) -> Unit)? = null,
    onSetSleepTimer: (Int) -> Unit = {},
    onSetSleepTimerEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onAddToPlaylist: ((Track) -> Unit)? = null,
    audioEffectManager: AudioEffectManager? = null,
    audioOutputManager: AudioOutputManager? = null,
    appSettings: AppSettings? = null,
    motionVideoPath: String? = null,
    motionPlayer: androidx.media3.exoplayer.ExoPlayer? = null,
    onRemoveMotionArtwork: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(appSettings?.isKeepScreenOnEnabled) {
        val shouldKeepOn = appSettings?.isKeepScreenOnEnabled == true
        if (shouldKeepOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            // Chỉ xóa cờ nếu chính sheet này đã bật, tránh tắt nhầm cờ do nơi khác đặt
            if (shouldKeepOn) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    var controlsDeckHeightPx by remember {
        val initialPx = with(density) { 260.dp.roundToPx() }
        mutableIntStateOf(initialPx)
    }
    val controlsDeckHeightDp = remember(controlsDeckHeightPx, density) {
        with(density) { controlsDeckHeightPx.toDp() }
    }

    val track = playbackState.currentTrack
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK) }
    val dialogs = remember { NowPlayingDialogsState() }
    var favoriteToastMessage by remember { mutableStateOf<String?>(null) }

    var favoriteToastJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun triggerFavoriteToast(message: String) {
        favoriteToastMessage = message
        // Hủy hẹn giờ của lần trước để thông báo mới hiện đủ thời gian
        favoriteToastJob?.cancel()
        favoriteToastJob = scope.launch {
            delay(2500L)
            favoriteToastMessage = null
        }
    }

    val handleToggleFavorite: (String, Boolean) -> Unit = { trackId, currentlyFav ->
        onToggleFavorite(trackId)
        triggerFavoriteToast(if (currentlyFav) "Đã xóa khỏi phần ưa thích" else "Đã ưa thích")
    }

    val lyricsListState = rememberLazyListState()
    val queueListState = rememberLazyListState()

    val deckVisibility = rememberControlsDeckVisibility(
        lyricsListState = lyricsListState,
        queueListState = queueListState,
        centerView = centerView
    )

    val nowPlayingHazeState = remember { HazeState() }

    // Interactive Horizontal Pager for Seamless Album Artwork Transitions (xem state/ArtworkPagerSync.kt)
    val queue = playbackState.queue
    val pagerState = rememberArtworkPagerState(
        queue = queue,
        currentIndex = playbackState.currentIndex,
        centerView = centerView,
        onPlayQueueIndex = onPlayQueueIndex
    )

    var lastButtonSkipTimeMs by remember { mutableLongStateOf(0L) }
    val buttonThrottleMs = 350L

    // Current displayed track (Đồng bộ với targetPage trong suốt hoạt ảnh cuộn để không bị giật/nhảy thông tin giữa chừng)
    val displayedTrack = if (centerView == NowPlayingCenterView.ARTWORK && queue.isNotEmpty()) {
        val targetIdx = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
        if (targetIdx in queue.indices) queue[targetIdx] else (track ?: queue.firstOrNull())
    } else {
        track ?: (if (queue.isNotEmpty()) queue.firstOrNull() else null)
    }

    // Kéo-để-đóng, hoạt ảnh trượt vào/ra (xem state/NowPlayingSheetState.kt)
    val sheetState = rememberNowPlayingSheetState(
        onCollapse = onCollapse,
        isArtworkMode = { centerView == NowPlayingCenterView.ARTWORK }
    )

    fun Modifier.sheetDragToDismiss(enabled: Boolean = true): Modifier =
        then(sheetState.dragToDismissModifier(enabled) { pagerState.isScrollInProgress })

    // Câu đang hát + tự cuộn Lời / Hàng đợi (xem state/CenterViewAutoScroll.kt)
    val centerAutoScroll = rememberCenterViewAutoScroll(
        track = track,
        playbackState = playbackState,
        centerView = centerView,
        positionState = positionState,
        lyricsListState = lyricsListState,
        queueListState = queueListState
    )

    BackHandler {
        if (dialogs.showSleepTimerDialog) {
            dialogs.showSleepTimerDialog = false
        } else if (centerView != NowPlayingCenterView.ARTWORK) {
            centerView = NowPlayingCenterView.ARTWORK
        } else {
            sheetState.collapse()
        }
    }

    val dismissProgress = sheetState.dismissProgress
    val sheetScale = lerp(1f, 0.92f, dismissProgress)
    val sheetAlpha = lerp(1f, 0.82f, dismissProgress)
    val topCornerRadius = lerp(32f, 36f, dismissProgress).dp
    val bottomCornerRadius = lerp(0f, 32f, dismissProgress).dp
    val dynamicSheetShape = RoundedCornerShape(
        topStart = topCornerRadius,
        topEnd = topCornerRadius,
        bottomStart = bottomCornerRadius,
        bottomEnd = bottomCornerRadius
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (centerView == NowPlayingCenterView.ARTWORK) {
                    Modifier.nestedScroll(sheetState.nestedScrollConnection)
                } else Modifier
            )
            .sheetDragToDismiss(enabled = centerView == NowPlayingCenterView.ARTWORK)
            .graphicsLayer {
                translationY = sheetState.offsetY.value
                scaleX = sheetScale
                scaleY = sheetScale
                this.alpha = sheetAlpha
                transformOrigin = TransformOrigin(0.5f, 0.95f)
                shape = dynamicSheetShape
                clip = true
                shadowElevation = (28f * (1f - dismissProgress)).coerceAtLeast(0f)
            }
            .clip(dynamicSheetShape)
            .background(ObsidianBlack)
    ) {
        // LAYER 0, 1A, 1B: nền ảnh bìa + màu động, pager ảnh bìa, lớp voan kính mờ (xem backdrop/NowPlayingBackdrop.kt)
        NowPlayingBackdrop(
            isArtworkMode = centerView == NowPlayingCenterView.ARTWORK,
            displayedTrack = displayedTrack,
            track = track,
            queue = queue,
            pagerState = pagerState,
            isDynamicMeshBackgroundEnabled = appSettings?.isDynamicMeshBackgroundEnabled != false,
            motionVideoPath = motionVideoPath,
            motionPlayer = motionPlayer,
            hazeState = nowPlayingHazeState,
            isSheetFullyVisible = !sheetState.isDismissing,
            controlsDeckHeight = controlsDeckHeightDp,
            onArtworkLongClick = { dialogs.showTrackDetailsDialog = true }
        )

        // LAYER 2: ONE PAGE FLOATING INTERACTIVE LAYER (Pull handle, In-place Lyrics/Queue, and Collapsible Master Controls)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Gesture Spacer (Pull-down bar removed as requested, zero visual clutter)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .sheetDragToDismiss(enabled = true)
            )

            // SÂN KHẤU TRUNG TÂM (FLUID IN-PLACE OVERLAYS FOR LYRICS & QUEUE)
            NowPlayingCenterStage(
                centerView = centerView,
                track = track,
                playbackState = playbackState,
                positionState = positionState,
                lyricsListState = lyricsListState,
                queueListState = queueListState,
                deckVisibility = deckVisibility,
                activeLyricIndex = { centerAutoScroll.activeLyricIndex },
                onSeekToLine = { index, seekTime ->
                    centerAutoScroll.resetLyricsUserScroll()
                    scope.launch {
                        lyricsListState.animateScrollToItem(index)
                    }
                    onSeek(seekTime)
                },
                hazeState = nowPlayingHazeState,
                onPlayQueueIndex = onPlayQueueIndex,
                onMoveQueueItem = onMoveQueueItem,
                onRemoveQueueItem = onRemoveQueueItem,
                onClearPlaybackHistory = onClearPlaybackHistory,
                onToggleFavorite = handleToggleFavorite,
                onAddToPlaylist = onAddToPlaylist,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleAutoplay = onToggleAutoplay,
                onOpenSpeedMenu = { dialogs.isSpeedMenuOpen = true },
                onOpenSleepTimer = { dialogs.showSleepTimerDialog = true },
                onOpenDetails = { dialogs.showTrackDetailsDialog = true },
                onOpenOptions = { dialogs.showOptionsMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // BỘ ĐIỀU KHIỂN PHÁT NHẠC & DOCK ĐÁY (ẨN KHI CUỘN XUỐNG DUYỆT BÀI / ĐỌC LỜI, HIỆN KHI VUỐT LÊN TRÊN HOẶC ĐẦU TRANG)
            val isControlsDeckVisible = deckVisibility.isVisible(centerView)
            NowPlayingControlsDeck(
                visible = isControlsDeckVisible,
                centerView = centerView,
                displayedTrack = displayedTrack,
                track = track,
                playbackState = playbackState,
                positionState = positionState,
                appSettings = appSettings,
                isEqualizerOpen = dialogs.showEqualizerDialog,
                hazeState = nowPlayingHazeState,
                onDeckHeightMeasured = { controlsDeckHeightPx = it },
                onToggleFavorite = handleToggleFavorite,
                onOpenOptions = { dialogs.showOptionsMenu = true },
                onOpenTrackDetails = { dialogs.showTrackDetailsDialog = true },
                onSeek = onSeek,
                onPrevious = {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastButtonSkipTimeMs >= buttonThrottleMs) {
                        lastButtonSkipTimeMs = now
                        onPrevious()
                    }
                },
                onPlayPause = onPlayPause,
                onNext = {
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastButtonSkipTimeMs >= buttonThrottleMs) {
                        lastButtonSkipTimeMs = now
                        onNext()
                    }
                },
                onCenterViewChange = { centerView = it },
                onOpenEqualizer = { dialogs.showEqualizerDialog = true }
            )
        }

        // LAYER 3–7: tốc độ phát, hẹn giờ ngủ, thông tin bài, EQ, bảng thao tác bài (xem dialogs/NowPlayingDialogLayers.kt)
        NowPlayingDialogLayers(
            dialogs = dialogs,
            playbackState = playbackState,
            queue = queue,
            pagerState = pagerState,
            track = track,
            displayedTrack = displayedTrack,
            audioEffectManager = audioEffectManager,
            motionVideoPath = motionVideoPath,
            hazeState = nowPlayingHazeState,
            onSetPlaybackSpeed = onSetPlaybackSpeed,
            onSetSleepTimer = onSetSleepTimer,
            onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
            onCancelSleepTimer = onCancelSleepTimer,
            onToggleFavorite = handleToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onRemoveQueueItem = onRemoveQueueItem,
            onRemoveMotionArtwork = onRemoveMotionArtwork
        )

        // LAYER 8: Floating Favorite Toast Banner (1:1 Apple Music Floating Squircle Pill)
        FavoriteToastBanner(
            message = favoriteToastMessage,
            hazeState = nowPlayingHazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
