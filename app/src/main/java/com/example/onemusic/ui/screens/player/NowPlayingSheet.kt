package com.example.onemusic.ui.screens.player

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.media3.exoplayer.ExoPlayer
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.AudioOutputManager
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.playback.RepeatMode
import com.example.onemusic.data.model.Track
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.components.ApexEqualizerDialog
import com.example.onemusic.ui.components.ApexTrackActionSheet
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.utils.preloadArtworkAndColors
import com.example.onemusic.ui.utils.rememberArtworkColors
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern Fullscreen Music Player (Now Playing Sheet) - ONE PAGE ARCHITECTURE
 * - 100% Full-Bleed Edge-to-Edge Hero Cover & Motion Video (Permanently Mounted & CenterCrop Precision)
 * - Frosted Glass ("Nhám mờ") Backdrop Transition for Lyrics & Queue
 * - Collapsible Playback Controls Deck on Lyrics Scroll
 * - Clean Top Drag Handle [ — ] with Zero Clutter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    positionState: State<Long>,
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
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onMoveQueueItem: ((from: Int, to: Int) -> Unit)? = null,
    onRemoveQueueItem: ((Int) -> Unit)? = null,
    onPlayQueueIndex: (Int) -> Unit,
    audioEffectManager: AudioEffectManager? = null,
    audioOutputManager: AudioOutputManager? = null,
    onSetPlaybackSpeed: ((Float) -> Unit)? = null,
    onSetSleepTimer: (Int) -> Unit = {},
    onSetSleepTimerEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    appSettings: AppSettings? = null,
    motionVideoPath: String? = null,
    motionPlayer: ExoPlayer? = null,
    onRemoveMotionArtwork: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(appSettings?.isKeepScreenOnEnabled) {
        val shouldKeepOn = appSettings?.isKeepScreenOnEnabled == true
        if (shouldKeepOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (shouldKeepOn) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    var controlsDeckHeightPx by remember {
        val initialPx = with(density) { 260.dp.roundToPx() }
        mutableIntStateOf(initialPx)
    }
    val controlsDeckHeightDp = remember(controlsDeckHeightPx, density) {
        with(density) { controlsDeckHeightPx.toDp() }
    }

    // Physical drag state & smooth physics
    val sheetOffsetY = remember { Animatable(screenHeightPx) }
    val sheetSlideSpec = remember {
        spring<Float>(
            dampingRatio = 0.90f,
            stiffness = 280f
        )
    }

    val track = playbackState.currentTrack
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showTrackDetailsDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var isSpeedMenuOpen by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var favoriteToastMessage by remember { mutableStateOf<String?>(null) }
    var favoriteToastJob by remember { mutableStateOf<Job?>(null) }

    fun triggerFavoriteToast(message: String) {
        favoriteToastMessage = message
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

    // Dynamic Playback Deck Visibility on Lyrics Scroll
    var isLyricsDeckVisible by remember { mutableStateOf(true) }
    val isAtLyricsTop by remember {
        derivedStateOf {
            lyricsListState.firstVisibleItemIndex == 0 && lyricsListState.firstVisibleItemScrollOffset <= 30
        }
    }

    LaunchedEffect(isAtLyricsTop) {
        if (isAtLyricsTop) {
            isLyricsDeckVisible = true
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.LYRICS) {
            isLyricsDeckVisible = true
        }
    }

    val lyricsNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (source == NestedScrollSource.UserInput) {
                    if (deltaY < -12f) {
                        if (isLyricsDeckVisible && !isAtLyricsTop) {
                            isLyricsDeckVisible = false
                        }
                    } else if (deltaY > 12f) {
                        if (!isLyricsDeckVisible) {
                            isLyricsDeckVisible = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Dynamic Playback Deck Visibility on Queue Scroll
    var isQueueDeckVisible by remember { mutableStateOf(true) }
    val isAtQueueTop by remember {
        derivedStateOf {
            queueListState.firstVisibleItemIndex == 0 && queueListState.firstVisibleItemScrollOffset <= 15
        }
    }

    LaunchedEffect(isAtQueueTop) {
        if (isAtQueueTop) {
            isQueueDeckVisible = true
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.QUEUE) {
            isQueueDeckVisible = true
        }
    }

    val queueNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < -12f) {
                    if (isQueueDeckVisible && !isAtQueueTop) {
                        isQueueDeckVisible = false
                    }
                } else if (deltaY > 12f) {
                    if (!isQueueDeckVisible) {
                        isQueueDeckVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    val nowPlayingHazeState = remember { HazeState() }

    // Interactive Horizontal Pager for Seamless Album Artwork Transitions
    val queue = playbackState.queue
    val pageCount = if (queue.isNotEmpty()) queue.size else 1
    val initialPage = if (playbackState.currentIndex in queue.indices) playbackState.currentIndex else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    val currentPlaybackIndex by rememberUpdatedState(playbackState.currentIndex)
    val onPlayQueueIndexUpdated by rememberUpdatedState(onPlayQueueIndex)

    LaunchedEffect(currentPlaybackIndex) {
        if (queue.isNotEmpty() && currentPlaybackIndex in 0 until pageCount) {
            if (pagerState.currentPage != currentPlaybackIndex) {
                pagerState.scrollToPage(currentPlaybackIndex)
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (centerView == NowPlayingCenterView.ARTWORK && queue.isNotEmpty()) {
            val settled = pagerState.settledPage
            if (settled in queue.indices && settled != currentPlaybackIndex) {
                onPlayQueueIndexUpdated(settled)
            }
        }
    }

    // Preload Artwork & Colors for adjacent tracks
    LaunchedEffect(pagerState.currentPage, queue.size) {
        if (queue.isNotEmpty()) {
            val curr = pagerState.currentPage
            val nextIdx = (curr + 1).coerceAtMost(queue.lastIndex)
            val prevIdx = (curr - 1).coerceAtLeast(0)
            if (nextIdx != curr) {
                queue.getOrNull(nextIdx)?.artworkUrl?.let { preloadArtworkAndColors(context, it) }
            }
            if (prevIdx != curr && prevIdx != nextIdx) {
                queue.getOrNull(prevIdx)?.artworkUrl?.let { preloadArtworkAndColors(context, it) }
            }
        }
    }

    val displayedTrack = if (centerView == NowPlayingCenterView.ARTWORK && queue.isNotEmpty()) {
        val targetIdx = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
        if (targetIdx in queue.indices) queue[targetIdx] else (track ?: queue.firstOrNull())
    } else {
        track ?: (if (queue.isNotEmpty()) queue.firstOrNull() else null)
    }

    val dynamicArtworkColors = rememberArtworkColors(imageUrl = displayedTrack?.artworkUrl)
    val auroraColorEasing = remember { CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.00f) }

    val animatedTopColor by animateColorAsState(
        targetValue = remember(dynamicArtworkColors) { dynamicArtworkColors.topColor.copy(alpha = 1f) },
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_top_color"
    )
    val animatedSecondaryColor by animateColorAsState(
        targetValue = remember(dynamicArtworkColors) { dynamicArtworkColors.secondaryColor.copy(alpha = 1f) },
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_secondary_color"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = remember(dynamicArtworkColors) { dynamicArtworkColors.accentColor.copy(alpha = 1f) },
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_accent_color"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = remember(dynamicArtworkColors) { dynamicArtworkColors.bottomColor.copy(alpha = 1f) },
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_bottom_color"
    )

    // Smooth Entrance from Bottom
    LaunchedEffect(Unit) {
        sheetOffsetY.animateTo(0f, animationSpec = sheetSlideSpec)
    }

    var isDismissing by remember { mutableStateOf(false) }

    fun collapseSheet(initialVelocity: Float = 0f) {
        if (isDismissing) return
        isDismissing = true
        try {
            hapticEngine.performCrispTap(scale = 0.40f, fallbackView = currentView)
        } catch (_: Exception) {}
        scope.launch {
            sheetOffsetY.animateTo(
                targetValue = screenHeightPx,
                initialVelocity = initialVelocity.coerceAtLeast(0f),
                animationSpec = sheetSlideSpec
            )
            onCollapse()
        }
    }

    val dismissThresholdPx = screenHeightPx * 0.15f
    var hasFiredThresholdHaptic by remember { mutableStateOf(false) }

    val sheetNestedScrollConnection = rememberSheetNestedScrollConnection(
        sheetOffsetY = sheetOffsetY,
        isDismissing = { isDismissing },
        centerView = { centerView },
        dismissThresholdPx = dismissThresholdPx,
        sheetSlideSpec = sheetSlideSpec,
        scope = scope,
        hapticEngine = hapticEngine,
        currentView = currentView,
        onCollapse = { collapseSheet(it) },
        hasFiredThresholdHaptic = { hasFiredThresholdHaptic },
        setHasFiredThresholdHaptic = { hasFiredThresholdHaptic = it }
    )

    // Lyrics Auto-scroll calculation
    val parsedLyrics = displayedTrack?.lyrics ?: emptyList()
    val activeLyricIndex by remember(parsedLyrics) {
        derivedStateOf {
            if (parsedLyrics.isEmpty()) -1
            else {
                val currentPositionMs = positionState.value + 60L
                parsedLyrics.indexOfLast { it.timestampMs <= currentPositionMs }
            }
        }
    }

    var previousCenterView by remember { mutableStateOf(centerView) }
    var lastLyricsUserScrollTimeMs by remember { mutableLongStateOf(0L) }
    val isUserReadingLyrics by remember {
        derivedStateOf {
            val idleTimeMs = SystemClock.elapsedRealtime() - lastLyricsUserScrollTimeMs
            lyricsListState.isScrollInProgress || idleTimeMs < 4000L
        }
    }

    LaunchedEffect(lyricsListState.isScrollInProgress) {
        if (lyricsListState.isScrollInProgress) {
            lastLyricsUserScrollTimeMs = SystemClock.elapsedRealtime()
        }
    }

    LaunchedEffect(centerView) {
        if (previousCenterView != centerView) {
            if (centerView == NowPlayingCenterView.LYRICS) {
                lastLyricsUserScrollTimeMs = 0L
                if (activeLyricIndex >= 0 && activeLyricIndex < parsedLyrics.size) {
                    lyricsListState.scrollToItem(activeLyricIndex)
                }
            } else if (centerView == NowPlayingCenterView.QUEUE) {
                val curIdx = playbackState.currentIndex
                if (curIdx in playbackState.queue.indices) {
                    queueListState.scrollToItem((curIdx - 1).coerceAtLeast(0))
                }
            }
            previousCenterView = centerView
        }
    }

    LaunchedEffect(activeLyricIndex, isUserReadingLyrics, centerView) {
        if (centerView == NowPlayingCenterView.LYRICS && !isUserReadingLyrics) {
            if (activeLyricIndex >= 0 && activeLyricIndex < parsedLyrics.size) {
                lyricsListState.animateScrollToItem(activeLyricIndex)
            }
        }
    }

    var lastLyricsTrackId by remember { mutableStateOf(track?.id) }
    LaunchedEffect(track?.id) {
        if (track?.id != lastLyricsTrackId) {
            lastLyricsTrackId = track?.id
            lyricsListState.scrollToItem(0)
            lastLyricsUserScrollTimeMs = 0L
        }
    }

    LaunchedEffect(playbackState.currentIndex) {
        if (centerView == NowPlayingCenterView.QUEUE && playbackState.queue.isNotEmpty()) {
            if (!queueListState.isScrollInProgress && previousCenterView == NowPlayingCenterView.QUEUE) {
                queueListState.animateScrollToItem((playbackState.currentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    BackHandler {
        if (showSleepTimerDialog) {
            showSleepTimerDialog = false
        } else if (centerView != NowPlayingCenterView.ARTWORK) {
            centerView = NowPlayingCenterView.ARTWORK
        } else {
            collapseSheet()
        }
    }

    val dismissProgress = (sheetOffsetY.value / screenHeightPx).coerceIn(0f, 1f)
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

    val isArtworkMode = centerView == NowPlayingCenterView.ARTWORK
    val blurRadiusAnimated by animateDpAsState(
        targetValue = if (isArtworkMode) 0.dp else 26.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "nowplaying_bg_blur"
    )
    val frostedGlassAlpha by animateFloatAsState(
        targetValue = if (!isArtworkMode) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "frosted_glass_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (centerView == NowPlayingCenterView.ARTWORK) {
                    Modifier.nestedScroll(sheetNestedScrollConnection)
                } else Modifier
            )
            .sheetDragToDismiss(
                enabled = centerView == NowPlayingCenterView.ARTWORK,
                sheetOffsetY = sheetOffsetY,
                isDismissing = { isDismissing },
                isScrollInProgress = { pagerState.isScrollInProgress },
                dismissThresholdPx = dismissThresholdPx,
                sheetSlideSpec = sheetSlideSpec,
                scope = scope,
                hapticEngine = hapticEngine,
                currentView = currentView,
                onCollapse = { collapseSheet(it) },
                setHasFiredThresholdHaptic = { hasFiredThresholdHaptic = it }
            )
            .graphicsLayer {
                translationY = sheetOffsetY.value
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
        // LAYER 0: Dynamic Ambient Backdrop Mesh
        NowPlayingBackdrop(
            context = context,
            displayedTrack = displayedTrack,
            hazeState = nowPlayingHazeState,
            appSettings = appSettings,
            animatedTopColor = animatedTopColor,
            animatedSecondaryColor = animatedSecondaryColor,
            animatedAccentColor = animatedAccentColor,
            animatedBottomColor = animatedBottomColor,
            blurRadiusAnimated = blurRadiusAnimated
        )

        // LAYER 1A: Hero Album Artwork Carousel
        NowPlayingArtworkCarousel(
            pagerState = pagerState,
            queue = queue,
            track = track,
            motionVideoPath = motionVideoPath,
            motionPlayer = motionPlayer,
            artworkScale = 1.0f,
            isDismissing = isDismissing,
            hazeState = nowPlayingHazeState,
            appSettings = appSettings,
            animatedSecondaryColor = animatedSecondaryColor,
            controlsDeckHeightDp = controlsDeckHeightDp,
            onLongClick = { trk ->
                if (trk != null) showTrackDetailsDialog = true
            }
        )

        // LAYER 1B: Frosted Glass Veil when in Lyrics / Queue
        NowPlayingVeil(frostedGlassAlpha = frostedGlassAlpha)

        // LAYER 2: ONE PAGE FLOATING INTERACTIVE LAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top gesture spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .sheetDragToDismiss(
                        enabled = true,
                        sheetOffsetY = sheetOffsetY,
                        isDismissing = { isDismissing },
                        isScrollInProgress = { pagerState.isScrollInProgress },
                        dismissThresholdPx = dismissThresholdPx,
                        sheetSlideSpec = sheetSlideSpec,
                        scope = scope,
                        hapticEngine = hapticEngine,
                        currentView = currentView,
                        onCollapse = { collapseSheet(it) },
                        setHasFiredThresholdHaptic = { hasFiredThresholdHaptic = it }
                    )
            )

            // Sân khấu trung tâm: Fluid in-place overlays for Lyrics & Queue
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            initialState == NowPlayingCenterView.LYRICS && targetState == NowPlayingCenterView.QUEUE -> {
                                (slideInHorizontally(initialOffsetX = { (it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeIn(animationSpec = fadeSpec))
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { (-it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeOut(animationSpec = fadeOutSpec))
                            }
                            initialState == NowPlayingCenterView.QUEUE && targetState == NowPlayingCenterView.LYRICS -> {
                                (slideInHorizontally(initialOffsetX = { (-it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeIn(animationSpec = fadeSpec))
                                    .togetherWith(slideOutHorizontally(targetOffsetX = { (it * 0.35f).toInt() }, animationSpec = horizontalSpringSpec) + fadeOut(animationSpec = fadeOutSpec))
                            }
                            targetState == NowPlayingCenterView.ARTWORK -> {
                                fadeIn(animationSpec = fadeSpec).togetherWith(
                                    slideOutVertically(targetOffsetY = { (it * 0.65f).toInt() }, animationSpec = springSpec) + fadeOut(animationSpec = fadeOutSpec)
                                )
                            }
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
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        NowPlayingCenterView.LYRICS -> {
                            NowPlayingLyricsContent(
                                track = track,
                                lyricsListState = lyricsListState,
                                lyricsNestedScrollConnection = lyricsNestedScrollConnection,
                                positionState = positionState,
                                activeLyricIndex = activeLyricIndex,
                                onSeek = onSeek,
                                onUserSeekInLyrics = { idx, seekTime ->
                                    lastLyricsUserScrollTimeMs = 0L
                                    scope.launch {
                                        lyricsListState.animateScrollToItem(idx)
                                    }
                                    onSeek(seekTime)
                                }
                            )
                        }
                        NowPlayingCenterView.QUEUE -> {
                            NowPlayingQueueContent(
                                playbackState = playbackState,
                                queueListState = queueListState,
                                queueNestedScrollConnection = queueNestedScrollConnection,
                                onPlayQueueIndex = onPlayQueueIndex,
                                onMoveQueueItem = onMoveQueueItem,
                                onRemoveQueueItem = onRemoveQueueItem,
                                onClearPlaybackHistory = onClearPlaybackHistory,
                                onToggleFavorite = { trkId ->
                                    handleToggleFavorite(trkId, track?.isFavorite == true)
                                },
                                onAddToPlaylist = onAddToPlaylist,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat,
                                onToggleAutoplay = onToggleAutoplay,
                                onOpenSpeedMenu = { isSpeedMenuOpen = true },
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onOpenDetails = { showTrackDetailsDialog = true },
                                onOpenOptions = { showOptionsMenu = true },
                                hazeState = nowPlayingHazeState
                            )
                        }
                    }
                }
            }

            // Bottom Controls Deck: Animate visibility on scroll
            val shouldHideDeck = (centerView == NowPlayingCenterView.LYRICS && !isLyricsDeckVisible) ||
                                (centerView == NowPlayingCenterView.QUEUE && !isQueueDeckVisible)
            val deckAlpha by animateFloatAsState(
                targetValue = if (shouldHideDeck) 0.0f else 1.0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "controls_deck_alpha"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = deckAlpha }
                    .padding(horizontal = 24.dp)
                    .onSizeChanged { size ->
                        if (centerView == NowPlayingCenterView.ARTWORK) {
                            controlsDeckHeightPx = size.height
                        }
                    }
            ) {
                // Track Title, Artist & Options Menu (Ẩn khi đang ở QUEUE)
                if (centerView != NowPlayingCenterView.QUEUE) {
                    NowPlayingTrackInfoRow(
                        track = displayedTrack,
                        onToggleFavorite = handleToggleFavorite,
                        onOptionsClick = { showOptionsMenu = true }
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }

                // Progress Bar with Audio Badge
                val totalDurMs = if (playbackState.durationMs > 0) playbackState.durationMs else track?.durationMs ?: 0L
                NowPlayingProgressSection(
                    positionState = positionState,
                    totalDurMs = totalDurMs,
                    hasTrack = track != null,
                    onSeek = onSeek,
                    centerBadge = {
                        NowPlayingAudioBadge(
                            track = displayedTrack,
                            appSettings = appSettings,
                            onClick = { showTrackDetailsDialog = true },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(43.dp))

                // Master Playback Controls (Prev, Play/Pause, Next)
                NowPlayingMasterControls(
                    isPlaying = playbackState.isPlaying,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext
                )

                Spacer(modifier = Modifier.height(43.dp))

                // Bottom 3-Action Dock: Lyrics - Equalizer - Queue
                NowPlayingActionDock(
                    centerView = centerView,
                    isEqualizerOpen = showEqualizerDialog,
                    onSelectLyrics = { centerView = NowPlayingCenterView.LYRICS },
                    onOpenEqualizer = { showEqualizerDialog = true },
                    onSelectQueue = { centerView = NowPlayingCenterView.QUEUE },
                    onToggleBackToArtwork = { centerView = NowPlayingCenterView.ARTWORK },
                    hazeState = nowPlayingHazeState
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // LAYER 3: Playback Speed Selection Dialog
        if (isSpeedMenuOpen) {
            PlaybackSpeedDialog(
                playbackSpeed = playbackState.playbackSpeed,
                onDismissRequest = { isSpeedMenuOpen = false },
                onSpeedSelected = { sp -> onSetPlaybackSpeed?.invoke(sp) },
                hazeState = nowPlayingHazeState
            )
        }

        // LAYER 4: Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                playbackState = playbackState,
                onDismissRequest = { showSleepTimerDialog = false },
                onSetSleepTimer = onSetSleepTimer,
                onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
                onCancelSleepTimer = onCancelSleepTimer,
                hazeState = nowPlayingHazeState
            )
        }

        // LAYER 5: Track Audio Inspector Dialog
        val currentInspectedTrack = if (queue.isNotEmpty() && pagerState.currentPage in queue.indices) queue[pagerState.currentPage] else track
        if (showTrackDetailsDialog && currentInspectedTrack != null) {
            TrackDetailsDialog(
                track = currentInspectedTrack,
                onDismiss = { showTrackDetailsDialog = false }
            )
        }

        // LAYER 6: Equalizer & SoundAlive DSP Dialog
        if (showEqualizerDialog && audioEffectManager != null) {
            ApexEqualizerDialog(
                audioEffectManager = audioEffectManager,
                onDismiss = { showEqualizerDialog = false }
            )
        }

        // LAYER 7: Track Action Sheet
        val actionSheetTrack = displayedTrack ?: track
        if (showOptionsMenu && actionSheetTrack != null) {
            ApexTrackActionSheet(
                track = actionSheetTrack,
                onDismissRequest = { showOptionsMenu = false },
                onToggleFavorite = { trackId ->
                    handleToggleFavorite(trackId, actionSheetTrack.isFavorite)
                },
                onAddToPlaylist = onAddToPlaylist,
                onDeleteTrack = { trk ->
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
                onOpenCredits = { showTrackDetailsDialog = true },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                hasMotionArtwork = (motionVideoPath != null && track?.id == actionSheetTrack.id),
                onRemoveMotionArtwork = onRemoveMotionArtwork,
                hazeState = nowPlayingHazeState
            )
        }

        // LAYER 8: Floating Favorite Toast Banner
        NowPlayingFavoriteToast(
            message = favoriteToastMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
            hazeState = nowPlayingHazeState
        )
    }
}
