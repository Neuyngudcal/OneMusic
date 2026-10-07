package com.example.onemusic.ui.screens.player

import androidx.compose.material3.minimumInteractiveComponentSize

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.AudioOutputManager
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.components.ApexEqualizerDialog
import com.example.onemusic.ui.components.ApexTrackActionSheet
import com.example.onemusic.ui.components.AppleBackwardIcon
import com.example.onemusic.ui.components.AppleForwardIcon
import com.example.onemusic.ui.components.AppleLosslessIcon
import com.example.onemusic.ui.components.ApplePauseIcon
import com.example.onemusic.ui.components.ApplePlayIcon
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.screens.player.artwork.MotionArtworkPlayer
import com.example.onemusic.ui.screens.player.artwork.StaticAlbumArtwork
import com.example.onemusic.ui.screens.player.controls.NowPlayingActionDock
import com.example.onemusic.ui.screens.player.controls.NowPlayingProgressSection
import com.example.onemusic.ui.screens.player.lyrics.WordByWordLyricItem
import com.example.onemusic.ui.screens.player.lyrics.isInstrumentalLine
import com.example.onemusic.ui.screens.player.queue.QueueFlatTrackRow
import com.example.onemusic.ui.screens.player.queue.QueuePlaybackModesRow
import com.example.onemusic.ui.screens.player.queue.QueueTopHeader
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration
import com.example.onemusic.ui.utils.rememberArtworkColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryBody
import com.example.onemusic.theme.IvoryDisabled
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextDisabled
import com.example.onemusic.theme.apexFrostedGlass

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

    val hapticEngine = com.example.onemusic.haptics.rememberApexHaptics()
    val currentView = androidx.compose.ui.platform.LocalView.current
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
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

    // Dynamic Playback Deck Visibility on Lyrics Scroll: Giống bên danh sách chờ (Cuộn xuống dưới ẩn, cuộn lên trên hiện, ở đầu trang luôn hiện)
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
                    // Kéo lên (cuộn xuống dưới để đọc tiếp lời): ẩn cụm playing
                    if (deltaY < -12f) {
                        if (isLyricsDeckVisible && !isAtLyricsTop) {
                            isLyricsDeckVisible = false
                        }
                    }
                    // Kéo xuống (cuộn ngược lên trên): hiện cụm playing
                    else if (deltaY > 12f) {
                        if (!isLyricsDeckVisible) {
                            isLyricsDeckVisible = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Dynamic Playback Deck Visibility on Queue Scroll: Cuộn xuống dưới để xem thêm bài thì ẨN cụm playing, cuộn lên trên thì HIỆN cụm playing
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
                // Kéo lên (cuộn xuống dưới để xem bài): ẩn cụm playing
                if (deltaY < -12f) {
                    if (isQueueDeckVisible && !isAtQueueTop) {
                        isQueueDeckVisible = false
                    }
                }
                // Kéo xuống (cuộn ngược lên trên): hiện cụm playing
                else if (deltaY > 12f) {
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

    var lastButtonSkipTimeMs by remember { mutableLongStateOf(0L) }
    val buttonThrottleMs = 350L

    // Synchronize external playback changes to Pager with Snappy 280ms Transition
    LaunchedEffect(playbackState.currentIndex, pageCount, centerView) {
        val target = playbackState.currentIndex
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
    LaunchedEffect(playbackState.currentIndex, queue) {
        if (queue.isNotEmpty()) {
            val curr = playbackState.currentIndex
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

    // Current displayed track (Đồng bộ với targetPage trong suốt hoạt ảnh cuộn để không bị giật/nhảy thông tin giữa chừng)
    val displayedTrack = if (centerView == NowPlayingCenterView.ARTWORK && queue.isNotEmpty()) {
        val targetIdx = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
        if (targetIdx in queue.indices) queue[targetIdx] else (track ?: queue.firstOrNull())
    } else {
        track ?: (if (queue.isNotEmpty()) queue.firstOrNull() else null)
    }

    // Dynamic Artwork Colors with Swatch Fallback & HSL (Synchronized with displayedTrack)
    val dynamicArtworkColors = rememberArtworkColors(imageUrl = displayedTrack?.artworkUrl)

    val targetTopColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.topColor.copy(alpha = 1f)
    }
    val targetSecondaryColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.secondaryColor.copy(alpha = 1f)
    }
    val targetAccentColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.accentColor.copy(alpha = 1f)
    }
    val targetBottomColor = remember(dynamicArtworkColors) {
        dynamicArtworkColors.bottomColor.copy(alpha = 1f)
    }

    val auroraColorEasing = remember { CubicBezierEasing(0.25f, 0.10f, 0.25f, 1.00f) }

    val animatedTopColor by animateColorAsState(
        targetValue = targetTopColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_top_color"
    )
    val animatedSecondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_secondary_color"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 600, easing = auroraColorEasing),
        label = "bg_accent_color"
    )
    val animatedBottomColor by animateColorAsState(
        targetValue = targetBottomColor,
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

    val sheetNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = sheetOffsetY.value
                if (currentOffset > 0f && available.y < 0f && centerView == NowPlayingCenterView.ARTWORK) {
                    val newOffset = (currentOffset + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - currentOffset
                    scope.launch { sheetOffsetY.snapTo(newOffset) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && !isDismissing && centerView == NowPlayingCenterView.ARTWORK) {
                    val newOffset = (sheetOffsetY.value + available.y).coerceAtLeast(0f)
                    scope.launch { sheetOffsetY.snapTo(newOffset) }

                    if (!hasFiredThresholdHaptic && newOffset >= dismissThresholdPx) {
                        try {
                            hapticEngine.performSpringLatch(scale = 0.45f, fallbackView = currentView)
                        } catch (_: Exception) {}
                        hasFiredThresholdHaptic = true
                    } else if (hasFiredThresholdHaptic && newOffset < dismissThresholdPx * 0.8f) {
                        hasFiredThresholdHaptic = false
                    }

                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffsetY.value > 0f && !isDismissing && centerView == NowPlayingCenterView.ARTWORK) {
                    val velocityY = available.y
                    val shouldDismiss = when {
                        velocityY > 800f -> true
                        velocityY < -800f -> false
                        sheetOffsetY.value > dismissThresholdPx -> true
                        else -> false
                    }
                    if (shouldDismiss) {
                        collapseSheet(initialVelocity = velocityY)
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityY.coerceAtMost(0f),
                            animationSpec = sheetSlideSpec
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f && !isDismissing && centerView == NowPlayingCenterView.ARTWORK) {
                    val velocityY = available.y
                    val shouldDismiss = when {
                        velocityY > 800f -> true
                        sheetOffsetY.value > dismissThresholdPx -> true
                        else -> false
                    }
                    if (shouldDismiss) {
                        collapseSheet(initialVelocity = velocityY)
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityY.coerceAtMost(0f),
                            animationSpec = sheetSlideSpec
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // derivedStateOf: chỉ báo thay đổi khi SANG CÂU MỚI, không phải mỗi 40ms khi vị trí đổi
    val lyricLines = track?.lyrics.orEmpty()
    val activeLyricIndex by remember(lyricLines) {
        derivedStateOf {
            // indexOfLast trả về -1 khi chưa tới câu nào (đoạn nhạc dạo) → không dòng nào sáng
            if (lyricLines.isEmpty()) -1
            else lyricLines.indexOfLast { it.timestampMs <= positionState.value + 60L }
        }
    }

    // Theo dõi trạng thái trước đó để phát hiện khoảnh khắc vừa chuyển sang LYRICS hoặc QUEUE
    var previousCenterView by remember { mutableStateOf(centerView) }

    // User drag detection for lyrics to avoid interrupting manual reading
    val isLyricsDragged by lyricsListState.interactionSource.collectIsDraggedAsState()
    var lastLyricsUserScrollTimeMs by remember { mutableLongStateOf(0L) }

    // Tính "người dùng đang đọc" từ lúc cuộn DỪNG HẲN (kể cả sau khi hất/fling), không phải lúc nhấc tay
    var isUserScrollingLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(isLyricsDragged) {
        if (isLyricsDragged) {
            isUserScrollingLyrics = true
            lastLyricsUserScrollTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(lyricsListState.isScrollInProgress) {
        if (!lyricsListState.isScrollInProgress && isUserScrollingLyrics) {
            isUserScrollingLyrics = false
            lastLyricsUserScrollTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(centerView) {
        if (centerView == NowPlayingCenterView.LYRICS && previousCenterView != NowPlayingCenterView.LYRICS) {
            // Nhảy ngay lập tức đến câu hát hiện tại vào trọng tâm quang học
            if (track != null && track.lyrics.isNotEmpty() && activeLyricIndex in track.lyrics.indices) {
                lyricsListState.scrollToItem(activeLyricIndex)
            }
        } else if (centerView == NowPlayingCenterView.QUEUE && previousCenterView != NowPlayingCenterView.QUEUE) {
            // Nhảy ngay lập tức đến bài hát đang phát trong hàng đợi
            if (playbackState.queue.isNotEmpty() && playbackState.currentIndex in playbackState.queue.indices) {
                queueListState.scrollToItem((playbackState.currentIndex - 1).coerceAtLeast(0))
            }
        }
        previousCenterView = centerView
    }

    LaunchedEffect(activeLyricIndex) {
        // Chỉ chạy animation cuộn định tâm khi đang ở màn hình Lyrics và người dùng không đang tự cuộn
        if (centerView == NowPlayingCenterView.LYRICS && track != null && track.lyrics.isNotEmpty() && activeLyricIndex in track.lyrics.indices) {
            val isUserReadingAhead = isLyricsDragged || (System.currentTimeMillis() - lastLyricsUserScrollTimeMs < 3500L)
            if (!isUserReadingAhead && !lyricsListState.isScrollInProgress && previousCenterView == NowPlayingCenterView.LYRICS) {
                lyricsListState.animateScrollToItem(activeLyricIndex)
            }
        }
    }

    // Đổi bài → đưa danh sách lời về đầu, bỏ trạng thái "đang đọc" của bài cũ.
    // Bỏ qua lần chạy đầu để không đè lên việc cuộn tới câu đang hát khi mở sheet ở tab Lời.
    var lastLyricsTrackId by remember { mutableStateOf(track?.id) }
    LaunchedEffect(track?.id) {
        if (track?.id != lastLyricsTrackId) {
            lastLyricsTrackId = track?.id
            lyricsListState.scrollToItem(0)
            lastLyricsUserScrollTimeMs = 0L
        }
    }

    LaunchedEffect(playbackState.currentIndex) {
        // Cuộn mượt mà đến bài hát mới khi đổi bài trong lúc đang xem hàng đợi
        if (centerView == NowPlayingCenterView.QUEUE && playbackState.queue.isNotEmpty()) {
            if (!queueListState.isScrollInProgress && previousCenterView == NowPlayingCenterView.QUEUE) {
                queueListState.animateScrollToItem((playbackState.currentIndex - 1).coerceAtLeast(0))
            }
        }
    }

    fun Modifier.sheetDragToDismiss(enabled: Boolean = true): Modifier = if (!enabled) this else this.pointerInput(enabled) {
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            hasFiredThresholdHaptic = false
            var isDragging = false
            var isHorizontalLocked = false
            var totalDx = 0f
            var totalDy = 0f
            // Giữ vị trí trong biến cục bộ của cử chỉ: snapTo chạy bất đồng bộ nên đọc sheetOffsetY.value
            // ngay sau khi launch sẽ ra giá trị cũ → cộng dồn lệch, sheet bị rung
            var localOffset = sheetOffsetY.value

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val dragChange = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!dragChange.pressed) {
                    break
                }

                velocityTracker.addPosition(dragChange.uptimeMillis, dragChange.position)
                val delta = dragChange.positionChange()
                totalDx += kotlin.math.abs(delta.x)
                totalDy += delta.y

                // Phân luồng cử chỉ (Directional Disambiguation): Nếu người dùng đang vuốt ngang thì khóa không cho kéo sheet
                if (!isDragging && !isHorizontalLocked) {
                    if (totalDx > 12f && totalDx > totalDy * 1.1f) {
                        isHorizontalLocked = true
                    } else if (totalDy > 16f && totalDy > totalDx * 1.8f && !pagerState.isScrollInProgress) {
                        isDragging = true
                    }
                }

                if (isDragging && !isHorizontalLocked && !isDismissing) {
                    dragChange.consume()
                    if (delta.y > 0 || localOffset > 0f) {
                        localOffset = (localOffset + delta.y).coerceAtLeast(0f)
                        val newOffset = localOffset
                        scope.launch {
                            sheetOffsetY.snapTo(newOffset)
                        }

                        if (!hasFiredThresholdHaptic && newOffset >= dismissThresholdPx) {
                            try {
                                hapticEngine.performSpringLatch(scale = 0.45f, fallbackView = currentView)
                            } catch (_: Exception) {}
                            hasFiredThresholdHaptic = true
                        } else if (hasFiredThresholdHaptic && newOffset < dismissThresholdPx * 0.8f) {
                            hasFiredThresholdHaptic = false
                        }
                    }
                }
            }

            if (isDragging && !isDismissing) {
                val velocityY = velocityTracker.calculateVelocity().y
                val currentOffset = localOffset
                val shouldDismiss = when {
                    velocityY > 800f -> true
                    velocityY < -800f -> false
                    currentOffset > dismissThresholdPx -> true
                    else -> false
                }

                scope.launch {
                    if (shouldDismiss) {
                        collapseSheet(initialVelocity = velocityY)
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            initialVelocity = velocityY.coerceAtMost(0f),
                            animationSpec = sheetSlideSpec
                        )
                    }
                }
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


    // Quản lý chuyển cảnh Kính Mờ Quang Học Trên Toàn Bộ Giao Diện Now Playing (120fps)
    val isArtworkMode = centerView == NowPlayingCenterView.ARTWORK

    val artworkScale = 1.0f
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
            .sheetDragToDismiss(enabled = centerView == NowPlayingCenterView.ARTWORK)
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
        // LAYER 0 & 1A: TRUE NOW PLAYING LIVING BACKDROP (AMBIENT MESH + HERO ALBUM ARTWORK)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurRadiusAnimated > 0.5.dp) {
                        Modifier.blur(blurRadiusAnimated)
                    } else Modifier
                )
        ) {
            // LAYER 0: Haze Source Background Canvas (Album Art Blur + Vibrant Mesh Gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = nowPlayingHazeState)
                    .background(animatedBottomColor)
            ) {
            if (appSettings?.isDynamicMeshBackgroundEnabled != false) {
                // 1. Phóng to ảnh album + làm mờ (Blur 50dp) với hiệu ứng chuyển đổi mờ dần 600ms siêu mượt
                AnimatedContent(
                    targetState = displayedTrack?.artworkUrl,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) togetherWith fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing))
                    },
                    label = "bg_art_blur_crossfade",
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 50.dp)
                ) { artUrl ->
                    if (!artUrl.isNullOrBlank()) {
                        val bgImageRequest = remember(artUrl) {
                            ImageRequest.Builder(context)
                                .data(artUrl)
                                .crossfade(false)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build()
                        }
                        AsyncImage(
                            model = bgImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = 0.45f
                                    scaleX = 1.25f
                                    scaleY = 1.25f
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.00f to Color.Black,
                                            0.30f to Color.Black,
                                            0.50f to Color.Black.copy(alpha = 0.85f),
                                            0.70f to Color.Black.copy(alpha = 0.55f),
                                            0.85f to Color.Black.copy(alpha = 0.25f),
                                            0.96f to Color.Black.copy(alpha = 0.05f),
                                            1.00f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )
                    }
                }

                // 2. Dynamic Ambient Gradient Mesh (Linear, Radial & Warm Organic Atmosphere)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // A. Linear Gradient trải dài toàn màn hình từ trên xuống theo dải màu hữu cơ
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to animatedTopColor.copy(alpha = 0.85f),
                                    0.35f to animatedTopColor.copy(alpha = 0.75f),
                                    0.65f to animatedSecondaryColor.copy(alpha = 0.78f),
                                    1.00f to animatedBottomColor.copy(alpha = 0.92f)
                                )
                            )

                            // B. Radial Gradient rực rỡ tỏa rộng quanh khu vực đĩa nhạc với màu Accent sống động
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedAccentColor.copy(alpha = 0.45f),
                                        animatedTopColor.copy(alpha = 0.30f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.25f),
                                    radius = size.height * 0.65f
                                )
                            )

                            // C. Lớp làm dịu nhẹ nhàng ở chân máy để giữ độ tương phản cho phím bấm trắng
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    0.70f to Color.Transparent,
                                    1.00f to Color.Black.copy(alpha = 0.32f)
                                )
                            )
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBlack)
                )
            }
        }

            // LAYER 1A: HERO ALBUM ARTWORK CAROUSEL (FULL BLEED TRÀN VIỀN TỪ ĐỈNH MÁY, ĐỒNG BỘ 100% VỚI CỤM PHÍM ĐÁY)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // SÂN KHẤU TRUNG TÂM (Tràn từ đỉnh máy y = 0 đến ngay trên cụm phím điều khiển)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 16.dp,
                        beyondViewportPageCount = 1,
                        key = { page -> if (queue.isNotEmpty() && page in queue.indices) "${queue[page].id}_$page" else "single_art" }
                    ) { page ->
                        val currentTrack = if (queue.isNotEmpty() && page in queue.indices) queue[page] else track

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                    this.alpha = lerp(0.35f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val isCurrentOrTargetPage = page == pagerState.currentPage || page == pagerState.targetPage
                            val targetVideoPath = if (isCurrentOrTargetPage && currentTrack?.id == track?.id) motionVideoPath else null

                            // FULL BLEED HERO ARTWORK CONTAINER
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artworkScale
                                        scaleY = artworkScale
                                    }
                                    .apexBounceClick(
                                        scaleDown = 0.98f,
                                        enableHaptic = true,
                                        onLongClick = {
                                            if (currentTrack != null) {
                                                showTrackDetailsDialog = true
                                            }
                                        }
                                    )
                            ) {
                                val activeScrimColor = if (appSettings?.isDynamicMeshBackgroundEnabled != false) {
                                    animatedSecondaryColor
                                } else {
                                    ObsidianBlack
                                }

                                if (targetVideoPath != null) {
                                    MotionArtworkPlayer(
                                        motionVideoPath = targetVideoPath,
                                        motionPlayer = motionPlayer,
                                        track = currentTrack,
                                        hazeState = nowPlayingHazeState,
                                        isSheetFullyVisible = !isDismissing,
                                        scrimColor = activeScrimColor
                                    )
                                } else if (currentTrack != null && currentTrack.artworkUrl.isNotBlank()) {
                                    StaticAlbumArtwork(
                                        context = context,
                                        track = currentTrack,
                                        applyMask = true,
                                        scrimColor = activeScrimColor
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = TextDisabled,
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Khoảng đệm bảo lưu kích thước cụm phím điều khiển (Đồng bộ không gian chân trang với Layer 2)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(controlsDeckHeightDp)
                )
            }
        }

        // LAYER 1B: AUTHENTIC OBSIDIAN FROSTED GLASS VEIL (Phủ voan than chì mờ thấu quang khi vào Lời bài hát / Hàng đợi)
        if (frostedGlassAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = frostedGlassAlpha }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
        }

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
                            val lyrics = track?.lyrics ?: emptyList()
                            val isLyricsSynced = remember(lyrics) {
                                lyrics.isNotEmpty() && lyrics.any { it.isSynced }
                            }

                            if (lyrics.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lyrics,
                                            contentDescription = null,
                                            tint = IvoryDisabled,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Text(
                                            text = "Không tìm thấy lời bài hát",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = IvoryFaint,
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                }
                            } else if (!isLyricsSynced) {
                                // APPLE MUSIC STATIC LYRICS SHEET (Unsynchronized plain text view)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = lyricsListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(lyricsNestedScrollConnection)
                                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0.00f to Color.Black,
                                                        0.82f to Color.Black,
                                                        1.00f to Color.Transparent
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            },
                                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 48.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .clip(PillShape)
                                                    .background(IvorySubtle)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lyrics,
                                                    contentDescription = null,
                                                    tint = IvoryMedium,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "Lời bài hát (Chưa đồng bộ)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = IvoryMedium,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        itemsIndexed(
                                            items = lyrics,
                                            key = { index, line -> "plain_${line.text.hashCode()}_$index" }
                                        ) { _, line ->
                                            if (isInstrumentalLine(line.text)) {
                                                Row(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    repeat(3) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(IvoryDisabled)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = line.text,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        color = IvoryHigh,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 32.sp,
                                                        lineHeight = 44.sp,
                                                        letterSpacing = (-0.4).sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // APPLE MUSIC TIME-SYNCED & ENHANCED KARAOKE LYRICS VIEW
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val topPadding = 24.dp
                                    val bottomPadding = 48.dp

                                    LazyColumn(
                                        state = lyricsListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(lyricsNestedScrollConnection)
                                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0.00f to Color.Black,
                                                        0.82f to Color.Black,
                                                        1.00f to Color.Transparent
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            },
                                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = topPadding, bottom = bottomPadding),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        itemsIndexed(
                                            items = lyrics,
                                            key = { index, line -> "${line.timestampMs}_$index" }
                                        ) { index, line ->
                                            val isActive = index == activeLyricIndex
                                            val distanceFromActive = kotlin.math.abs(index - activeLyricIndex)
                                            WordByWordLyricItem(
                                                lyric = line,
                                                isActive = isActive,
                                                distanceFromActive = distanceFromActive,
                                                positionState = positionState,
                                                onSeek = { seekTime ->
                                                    lastLyricsUserScrollTimeMs = 0L
                                                    scope.launch {
                                                        lyricsListState.animateScrollToItem(index)
                                                    }
                                                    onSeek(seekTime)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        NowPlayingCenterView.QUEUE -> {
                            val queueItems = playbackState.queue
                            if (queueItems.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                            contentDescription = null,
                                            tint = IvoryDisabled,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Text(
                                            text = "Hàng đợi phát trống",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = IvoryFaint,
                                                fontSize = 15.sp
                                            )
                                        )
                                    }
                                }
                            } else {
                                val currentIndex = playbackState.currentIndex.coerceIn(-1, queueItems.size - 1)
                                val currentTrack = playbackState.currentTrack ?: queueItems.getOrNull(currentIndex.coerceAtLeast(0))

                                val historyItems = remember(queueItems, currentIndex) {
                                    if (currentIndex > 0 && currentIndex <= queueItems.size) {
                                        queueItems.subList(0, currentIndex)
                                    } else {
                                        emptyList()
                                    }
                                }

                                val upNextItems = remember(queueItems, currentIndex) {
                                    if (currentIndex in queueItems.indices && currentIndex + 1 < queueItems.size) {
                                        queueItems.subList(currentIndex + 1, queueItems.size)
                                    } else if (currentIndex == -1) {
                                        queueItems
                                    } else {
                                        emptyList()
                                    }
                                }
                                // Key ổn định theo danh tính bài hát (không phụ thuộc vị trí), tính trước một lần O(n)
                                // thay vì mỗi key lặp lại cả danh sách (O(n²)). "occ" = số lần track.id đã xuất hiện trước đó,
                                // đảm bảo key duy nhất khi hàng đợi có 2 bài trùng track.id.
                                val upNextKeys = remember(upNextItems) {
                                    val seen = HashMap<String, Int>()
                                    upNextItems.map { t ->
                                        val occ = seen.getOrDefault(t.id, 0)
                                        seen[t.id] = occ + 1
                                        "next_${t.id}_occ$occ"
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 1. Apple Music Style Mini Playing Header
                                    QueueTopHeader(
                                        track = currentTrack,
                                        isPlaying = playbackState.isPlaying,
                                        onToggleFavorite = { trkId ->
                                            handleToggleFavorite(trkId, currentTrack?.isFavorite == true)
                                        },
                                        onAddToPlaylist = onAddToPlaylist,
                                        isShuffle = playbackState.isShuffle,
                                        onToggleShuffle = onToggleShuffle,
                                        repeatMode = playbackState.repeatMode,
                                        onCycleRepeat = onCycleRepeat,
                                        playbackSpeed = playbackState.playbackSpeed,
                                        onOpenSpeedMenu = { isSpeedMenuOpen = true },
                                        onOpenSleepTimer = { showSleepTimerDialog = true },
                                        onOpenDetails = { showTrackDetailsDialog = true },
                                        onOpenOptions = { showOptionsMenu = true },
                                        hazeState = nowPlayingHazeState
                                    )

                                    // 2. Playback Modes 3 Pills (Shuffle, Repeat, Autoplay)
                                    QueuePlaybackModesRow(
                                        isShuffle = playbackState.isShuffle,
                                        repeatMode = playbackState.repeatMode,
                                        isAutoplay = playbackState.isAutoplay,
                                        onToggleShuffle = onToggleShuffle,
                                        onCycleRepeat = onCycleRepeat,
                                        onToggleAutoplay = { onToggleAutoplay?.invoke() }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3. Scrollable List containing History & Up Next
                                    LazyColumn(
                                        state = queueListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .nestedScroll(queueNestedScrollConnection)
                                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        0.00f to Color.Transparent,
                                                        0.02f to Color.Black,
                                                        0.92f to Color.Black,
                                                        1.00f to Color.Transparent
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            },
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Phân vùng: Lịch sử (History Section)
                                        if (historyItems.isNotEmpty()) {
                                            item(key = "header_history") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Lịch sử",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            fontSize = 19.sp,
                                                            letterSpacing = (-0.3).sp
                                                        )
                                                    )

                                                    // Nút Xóa Lịch Sử
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(PillShape)
                                                            .background(IvorySubtle)
                                                            .apexBounceClick(scaleDown = 0.92f, enableHaptic = true) {
                                                                onClearPlaybackHistory?.invoke()
                                                            }
                                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Xóa",
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                color = TextPrimary,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            itemsIndexed(
                                                items = historyItems,
                                                key = { index, hTrack -> "hist_${hTrack.id}_$index" }
                                            ) { hIndex, hTrack ->
                                                QueueFlatTrackRow(
                                                    track = hTrack,
                                                    showReorder = false,
                                                    onClick = {
                                                        onPlayQueueIndex(hIndex)
                                                    }
                                                )
                                            }

                                            item(key = "divider_history_upnext") {
                                                Spacer(modifier = Modifier.height(14.dp))
                                            }
                                        }

                                        // Phân vùng: Tiếp tục phát (Up Next Section)
                                        item(key = "header_up_next") {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "Tiếp tục phát",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary,
                                                        fontSize = 19.sp,
                                                        letterSpacing = (-0.3).sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (playbackState.isAutoplay) "Tự động phát nhạc tương tự" else "Từ danh sách chờ",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = IvoryFaint,
                                                        fontSize = 13.sp
                                                    )
                                                )
                                            }
                                        }

                                        if (upNextItems.isEmpty()) {
                                            item(key = "empty_up_next") {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 24.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (playbackState.isAutoplay) "Hết bài hát trong danh sách chờ" else "Danh sách phát kết thúc",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = IvoryFaint,
                                                            fontSize = 14.sp
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            itemsIndexed(
                                                items = upNextItems,
                                                // FIX: Key phải ổn định theo danh tính bài hát (không phụ thuộc vị trí),
                                                // nếu không Compose sẽ coi mỗi bước kéo-sắp-xếp là 1 item hoàn toàn mới,
                                                // làm gãy animation "animateItem" và hủy giữa chừng gesture kéo (pointerInput bị restart).
                                                key = { index, _ -> upNextKeys[index] }
                                            ) { localIndex, nTrack ->
                                                val actualIndex = if (currentIndex in queueItems.indices) currentIndex + 1 + localIndex else localIndex
                                                val currentOnRemove = rememberUpdatedState(onRemoveQueueItem)
                                                val canDismiss = onRemoveQueueItem != null
                                                // FIX: Đọc các giá trị có thể đổi theo vị trí (actualIndex) qua rememberUpdatedState
                                                // để confirmValueChange (bị remember theo key hàng) và gesture kéo luôn dùng vị trí mới nhất.
                                                val latestActualIndex = rememberUpdatedState(actualIndex)

                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    positionalThreshold = { distance -> distance * 0.50f },
                                                    confirmValueChange = { dismissValue ->
                                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                            currentOnRemove.value?.invoke(latestActualIndex.value)
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }
                                                )

                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    enableDismissFromStartToEnd = false,
                                                    enableDismissFromEndToStart = canDismiss,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .animateItem(
                                                            fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                                                            fadeOutSpec = tween(200, easing = FastOutSlowInEasing),
                                                            placementSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
                                                        ),
                                                    backgroundContent = {
                                                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart &&
                                                                dismissState.currentValue == SwipeToDismissBoxValue.Settled
                                                        val progress = dismissState.progress

                                                        if (isSwiping) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(ApexRose.copy(alpha = (progress * 1.5f).coerceIn(0f, 1f)))
                                                                    .padding(horizontal = 20.dp),
                                                                contentAlignment = Alignment.CenterEnd
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Delete,
                                                                    contentDescription = "Xóa khỏi hàng đợi",
                                                                    tint = PrimaryIvory,
                                                                    modifier = Modifier.size(24.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    val view = LocalView.current
                                                    val density = LocalDensity.current
                                                    // Bước kéo = chiều cao thật của hàng + khoảng cách 2dp giữa các hàng, để hàng bám đúng ngón tay
                                                    var rowHeightPx by remember { mutableIntStateOf(0) }
                                                    val spacingPx = with(density) { 2.dp.toPx() }
                                                    val itemStepPx = if (rowHeightPx > 0) rowHeightPx + spacingPx else with(density) { 56.dp.toPx() }
                                                    // pointerInput(nTrack.id) không khởi động lại → đọc bước kéo mới nhất qua rememberUpdatedState
                                                    val latestItemStepPx = rememberUpdatedState(itemStepPx)
                                                    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

                                                    // FIX: Đọc các giá trị có thể đổi theo vị trí (danh giới hàng đợi)
                                                    // qua rememberUpdatedState để luôn lấy giá trị mới nhất mà KHÔNG cần khởi động lại
                                                    // gesture pointerInput giữa chừng khi người dùng đang kéo liên tục nhiều bậc.
                                                    val latestQueueLastIndex = rememberUpdatedState(queueItems.lastIndex)
                                                    val latestMinReorderIndex = rememberUpdatedState((playbackState.currentIndex + 1).coerceAtLeast(0))

                                                    QueueFlatTrackRow(
                                                        track = nTrack,
                                                        showReorder = true,
                                                        onClick = {
                                                            onPlayQueueIndex(actualIndex)
                                                        },
                                                        modifier = Modifier.onSizeChanged { rowHeightPx = it.height },
                                                        reorderContent = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .clip(CircleShape)
                                                                    // FIX: key theo track.id (ổn định) thay vì actualIndex (đổi liên tục khi kéo),
                                                                    // để gesture kéo-sắp-xếp không bị hủy giữa chừng sau mỗi bậc di chuyển.
                                                                    .pointerInput(nTrack.id) {
                                                                        detectVerticalDragGestures(
                                                                            onDragStart = {
                                                                                accumulatedDragY = 0f
                                                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                                            },
                                                                            onDragEnd = {
                                                                                accumulatedDragY = 0f
                                                                            },
                                                                            onDragCancel = {
                                                                                accumulatedDragY = 0f
                                                                            },
                                                                            onVerticalDrag = { change, dragAmount ->
                                                                                change.consume()
                                                                                accumulatedDragY += dragAmount

                                                                                val currentActualIndex = latestActualIndex.value
                                                                                val minReorderIndex = latestMinReorderIndex.value
                                                                                val stepPx = latestItemStepPx.value
                                                                                if (accumulatedDragY < -stepPx && currentActualIndex > minReorderIndex) {
                                                                                    onMoveQueueItem?.invoke(currentActualIndex, currentActualIndex - 1)
                                                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                                                    accumulatedDragY = 0f
                                                                                } else if (accumulatedDragY > stepPx && currentActualIndex < latestQueueLastIndex.value) {
                                                                                    onMoveQueueItem?.invoke(currentActualIndex, currentActualIndex + 1)
                                                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                                                    accumulatedDragY = 0f
                                                                                }
                                                                            }
                                                                        )
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Reorder,
                                                                    contentDescription = "Kéo để dời thứ tự",
                                                                    tint = IvoryFaint,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BỘ ĐIỀU KHIỂN PHÁT NHẠC & DOCK ĐÁY (ẨN KHI CUỘN XUỐNG DUYỆT BÀI / ĐỌC LỜI, HIỆN KHI VUỐT LÊN TRÊN HOẶC ĐẦU TRANG)
            val isControlsDeckVisible = when (centerView) {
                NowPlayingCenterView.LYRICS -> isLyricsDeckVisible || isAtLyricsTop
                NowPlayingCenterView.QUEUE -> isQueueDeckVisible || isAtQueueTop
                NowPlayingCenterView.ARTWORK -> true
            }
            AnimatedVisibility(
                visible = isControlsDeckVisible,
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
                                controlsDeckHeightPx = size.height
                            }
                        }
                ) {
                    // 1. Track Title, Artist & Options Menu (Ẩn khi đang mở Hàng đợi để tối ưu diện tích và tránh lặp thông tin)
                    if (centerView != NowPlayingCenterView.QUEUE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val legibilityTextShadow = remember {
                                    Shadow(color = ScrimColor, blurRadius = 8f)
                                }

                                Text(
                                    text = displayedTrack?.title ?: "Không phát",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            repeatDelayMillis = 2500,
                                            initialDelayMillis = 2000,
                                            spacing = androidx.compose.foundation.MarqueeSpacing(64.dp),
                                            velocity = 32.dp
                                        ),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = PrimaryIvory,
                                        fontSize = 23.sp,
                                        letterSpacing = (-0.3).sp,
                                        shadow = legibilityTextShadow
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayedTrack?.artist ?: "—",
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 2500,
                                        initialDelayMillis = 2000,
                                        spacing = androidx.compose.foundation.MarqueeSpacing(64.dp),
                                        velocity = 28.dp
                                    ),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = IvoryBody,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 16.sp,
                                        shadow = legibilityTextShadow
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Action Buttons: Favorite Star Button + 3-Dot More Menu Button (1:1 Apple Music)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Inverted Contrast Favorite Button (Solid white disc with black star when active)
                                val isCurrentTrackFav = displayedTrack?.isFavorite == true
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                        .size(35.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentTrackFav) PrimaryIvory else IvoryStroke)
                                        .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                                            displayedTrack?.let { trk ->
                                                handleToggleFavorite(trk.id, isCurrentTrackFav)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentTrackFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = if (isCurrentTrackFav) "Bỏ yêu thích" else "Yêu thích",
                                        tint = if (isCurrentTrackFav) CharcoalBlack else TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 2. Glass 3-dot More Options Button
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                                        .size(35.dp)
                                        .clip(CircleShape)
                                        .background(IvoryStroke)
                                        .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                                            showOptionsMenu = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "Tùy chọn",
                                        tint = PrimaryIvory,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    // Progress Capsule Scrub Bar
                    // FIX hiệu năng: thanh tua + 2 nhãn thời gian nằm trong NowPlayingProgressSection,
                    // chỉ composable đó đọc positionState → mỗi 40ms chỉ phần này vẽ lại, không phải cả sheet.
                    val totalDurMs = if (playbackState.durationMs > 0) playbackState.durationMs else track?.durationMs ?: 0L

                    // Lossless / Hi-Res Audio Tech Badge (Apple Music Precision Frosted capsule under scrubber)
                    val isFlacByName = remember(displayedTrack) {
                        displayedTrack?.let { trk ->
                            val urlLower = trk.audioUrl.lowercase()
                            urlLower.endsWith(".flac") ||
                            urlLower.contains(".flac?") ||
                            urlLower.contains(".flac/") ||
                            trk.bitRate.contains("flac", ignoreCase = true)
                        } ?: false
                    }
                    // contentResolver.getType là lệnh gọi hệ thống → chạy trên luồng IO thay vì luồng giao diện
                    val isFlacByMime by androidx.compose.runtime.produceState(initialValue = false, displayedTrack) {
                        val trk = displayedTrack
                        value = if (trk != null && !isFlacByName && trk.audioUrl.startsWith("content://")) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching {
                                    context.contentResolver.getType(android.net.Uri.parse(trk.audioUrl))?.contains("flac", ignoreCase = true) == true
                                }.getOrDefault(false)
                            }
                        } else {
                            false
                        }
                    }
                    val isFlacTrack = isFlacByName || isFlacByMime

                        // isFlacTrack có thể đổi sau khi produceState chạy xong → phải nằm trong key
                        val trackFormat = remember(displayedTrack, isFlacTrack) {
                            displayedTrack?.let { trk ->
                                val urlLower = trk.audioUrl.lowercase().substringBefore('?').substringBefore('#')
                                when {
                                    isFlacTrack -> "FLAC"
                                    urlLower.endsWith(".wav") || trk.bitRate.contains("wav", ignoreCase = true) -> "WAV"
                                    urlLower.endsWith(".alac") || trk.bitRate.contains("alac", ignoreCase = true) -> "ALAC"
                                    urlLower.endsWith(".aiff") || trk.bitRate.contains("aiff", ignoreCase = true) -> "AIFF"
                                    urlLower.endsWith(".dsd") || urlLower.endsWith(".dsf") || urlLower.endsWith(".dff") || trk.bitRate.contains("dsd", ignoreCase = true) -> "DSD"
                                    urlLower.endsWith(".mp3") || trk.bitRate.contains("mp3", ignoreCase = true) -> "MP3"
                                    urlLower.endsWith(".aac") || trk.bitRate.contains("aac", ignoreCase = true) -> "AAC"
                                    urlLower.endsWith(".m4a") || trk.bitRate.contains("m4a", ignoreCase = true) -> "M4A"
                                    urlLower.endsWith(".ogg") || trk.bitRate.contains("ogg", ignoreCase = true) -> "OGG"
                                    urlLower.endsWith(".opus") || trk.bitRate.contains("opus", ignoreCase = true) -> "OPUS"
                                    else -> {
                                        val ext = urlLower.substringAfterLast('.', "")
                                        if (ext.isNotBlank() && ext.length in 2..5 && !ext.contains('/')) ext.uppercase() else ""
                                    }
                                }
                            } ?: ""
                        }

                        val isHiResTrack = displayedTrack?.let { trk ->
                            trk.isHiRes ||
                            trk.bitRate.contains("hi-res", ignoreCase = true) ||
                            trk.bitRate.contains("24-bit", ignoreCase = true) ||
                            trk.bitRate.contains("96khz", ignoreCase = true) ||
                            trk.bitRate.contains("192khz", ignoreCase = true)
                        } ?: false

                        val isLosslessTrack = isFlacTrack || trackFormat in setOf("WAV", "ALAC", "AIFF", "DSD") || (displayedTrack?.bitRate?.contains("lossless", ignoreCase = true) == true)
                        val isHighQualityTrack = displayedTrack?.let { trk ->
                            trk.bitRate.contains("320", ignoreCase = true) ||
                            trk.bitRate.contains("256", ignoreCase = true)
                        } ?: false

                        // Biểu tượng Lossless chỉ hiển thị độc quyền cho file FLAC chất lượng cao
                        val hasAudioBadgeIcon = isFlacTrack

                        val audioBadgeText = when {
                            isHiResTrack -> "Hi-Res Lossless"
                            isLosslessTrack -> "Lossless"
                            isHighQualityTrack -> "High Quality"
                            trackFormat.isNotBlank() -> trackFormat
                            else -> "Lossless"
                        }

                        val showAudioBadge = (displayedTrack != null) && (appSettings?.isHiResBadgeEnabled != false)
                    NowPlayingProgressSection(
                        positionState = positionState,
                        totalDurMs = totalDurMs,
                        hasTrack = track != null,
                        onSeek = onSeek,
                        centerBadge = {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showAudioBadge,
                            modifier = Modifier.align(Alignment.Center),
                            enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f),
                            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.85f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(IvoryStroke)
                                    .apexBounceClick(
                                        scaleDown = 0.92f,
                                        enableHaptic = true,
                                        onClick = { showTrackDetailsDialog = true }
                                    )
                                    .padding(
                                        horizontal = if (hasAudioBadgeIcon) 9.dp else 11.dp,
                                        vertical = 2.5.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (hasAudioBadgeIcon) {
                                        AppleLosslessIcon(
                                            modifier = Modifier.size(width = 16.dp, height = 10.5.dp),
                                            tint = IvoryHigh
                                        )
                                    }
                                    Text(
                                        text = audioBadgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = IvoryHigh,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.1.sp
                                        )
                                    )
                                }
                            }
                        }
                        }
                    )

                    Spacer(modifier = Modifier.height(43.dp))

                    // Master Playback Controls (Apple Music Precision: Prev, Play/Pause, Next)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 52.dp)
                                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                                    val now = android.os.SystemClock.elapsedRealtime()
                                    if (now - lastButtonSkipTimeMs >= buttonThrottleMs) {
                                        lastButtonSkipTimeMs = now
                                        onPrevious()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AppleBackwardIcon(
                                modifier = Modifier.size(width = 50.dp, height = 29.dp),
                                tint = PrimaryIvory
                            )
                        }

                        // Play / Pause Central Button
                        Box(
                            modifier = Modifier
                                .size(width = 72.dp, height = 62.dp)
                                .apexBounceClick(scaleDown = 0.90f, enableHaptic = true) {
                                    onPlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (playbackState.isPlaying) {
                                ApplePauseIcon(
                                    modifier = Modifier.size(width = 32.dp, height = 38.dp),
                                    tint = PrimaryIvory
                                )
                            } else {
                                ApplePlayIcon(
                                    modifier = Modifier.size(width = 34.dp, height = 36.dp),
                                    tint = PrimaryIvory
                                )
                            }
                        }

                        // Next Button
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 52.dp)
                                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                                    val now = android.os.SystemClock.elapsedRealtime()
                                    if (now - lastButtonSkipTimeMs >= buttonThrottleMs) {
                                        lastButtonSkipTimeMs = now
                                        onNext()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AppleForwardIcon(
                                modifier = Modifier.size(width = 50.dp, height = 29.dp),
                                tint = PrimaryIvory
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(43.dp))

                    // 5. Bottom 3-Action Obsidian Island Dock: Lyrics (♫) - DSP (🎛) - Queue (🄯)
                    NowPlayingActionDock(
                        centerView = centerView,
                        isEqualizerOpen = showEqualizerDialog,
                        onSelectLyrics = {
                            centerView = NowPlayingCenterView.LYRICS
                        },
                        onOpenEqualizer = {
                            showEqualizerDialog = true
                        },
                        onSelectQueue = {
                            centerView = NowPlayingCenterView.QUEUE
                        },
                        onToggleBackToArtwork = {
                            centerView = NowPlayingCenterView.ARTWORK
                        },
                        hazeState = nowPlayingHazeState
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // LAYER 3: Playback Speed Selection Dialog
        if (isSpeedMenuOpen) {
            ApexDialogContainer(
                onDismissRequest = { isSpeedMenuOpen = false },
                hazeState = nowPlayingHazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Tốc độ phát",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(0.8.dp, SurfaceDivider, RoundedCornerShape(18.dp))
                            .background(SurfaceCard)
                    ) {
                        speedOptions.forEachIndexed { index, sp ->
                            val isSelected = playbackState.playbackSpeed == sp

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .apexBounceClick(scaleDown = 0.97f, enableHaptic = true) {
                                        onSetPlaybackSpeed?.invoke(sp)
                                        isSpeedMenuOpen = false
                                    }
                                    .padding(horizontal = 18.dp, vertical = 13.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${sp}x",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Brand else TextPrimary
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Brand,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            if (index < speedOptions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SurfaceDivider
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(SurfaceActiveIndicator)
                            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                isSpeedMenuOpen = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đóng",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }
            }
        }

        // LAYER 4: Sleep Timer Dialog
        if (showSleepTimerDialog) {
            ApexDialogContainer(
                onDismissRequest = { showSleepTimerDialog = false },
                hazeState = nowPlayingHazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Hẹn giờ tắt nhạc",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                    )

                    if (playbackState.sleepTimerRemainingSeconds != null) {
                        val rem = playbackState.sleepTimerRemainingSeconds
                        val display = if (rem < 0) "Sau khi kết thúc bài hát" else formatDuration(rem * 1000, padMinutes = true)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Đang chạy: $display",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Brand,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val timerOptions = listOf(
                        15 to "15 phút",
                        30 to "30 phút",
                        45 to "45 phút",
                        60 to "60 phút",
                        -1 to "Sau khi kết thúc bài hát",
                        0 to "Tắt hẹn giờ"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(0.8.dp, SurfaceDivider, RoundedCornerShape(18.dp))
                            .background(SurfaceCard)
                    ) {
                        timerOptions.forEachIndexed { index, (mins, label) ->
                            val isSelected = when (mins) {
                                0 -> playbackState.sleepTimerMinutes == null
                                -1 -> playbackState.sleepTimerMinutes == -1
                                else -> playbackState.sleepTimerMinutes == mins
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .apexBounceClick(scaleDown = 0.97f, enableHaptic = true) {
                                        when (mins) {
                                            0 -> onCancelSleepTimer()
                                            -1 -> onSetSleepTimerEndOfTrack()
                                            else -> onSetSleepTimer(mins)
                                        }
                                        showSleepTimerDialog = false
                                    }
                                    .padding(horizontal = 18.dp, vertical = 13.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Brand else TextPrimary
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Brand,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            if (index < timerOptions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = SurfaceDivider
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(SurfaceActiveIndicator)
                            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                                showSleepTimerDialog = false
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đóng",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        )
                    }
                }
            }
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

        // LAYER 7: OneMusic Apex Prism Track Action Sheet (1:1 Apple Music Modal Bottom Sheet)
        val actionSheetTrack = displayedTrack ?: track
        if (showOptionsMenu && actionSheetTrack != null) {
            ApexTrackActionSheet(
                track = actionSheetTrack,
                onDismissRequest = { showOptionsMenu = false },
                onToggleFavorite = { trackId ->
                    val isCurrentlyFav = actionSheetTrack.isFavorite
                    handleToggleFavorite(trackId, isCurrentlyFav)
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
                onOpenCredits = { showTrackDetailsDialog = true },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                hasMotionArtwork = (motionVideoPath != null && track?.id == actionSheetTrack.id),
                onRemoveMotionArtwork = onRemoveMotionArtwork,
                hazeState = nowPlayingHazeState
            )
        }

        // LAYER 8: Floating Favorite Toast Banner (1:1 Apple Music Floating Squircle Pill)
        AnimatedVisibility(
            visible = favoriteToastMessage != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .navigationBarsPadding(),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .border(0.7.dp, IvoryStroke, RoundedCornerShape(18.dp))
                    .apexFrostedGlass(
                        backgroundColor = SurfaceElevated.copy(alpha = 0.92f),
                        blurRadius = 24.dp,
                        hazeState = nowPlayingHazeState
                    )
                    .padding(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isFavoritedMsg = favoriteToastMessage?.contains("Đã ưa thích") == true
                    Icon(
                        imageVector = if (isFavoritedMsg) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = PrimaryIvory,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = favoriteToastMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        )
                    )
                }
            }
        }
    }
}
