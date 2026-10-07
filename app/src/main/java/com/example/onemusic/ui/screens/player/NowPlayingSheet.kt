package com.example.onemusic.ui.screens.player

import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.AudioOutputManager
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.ui.components.ApexEqualizerDialog
import com.example.onemusic.ui.components.ApexTrackActionSheet
import com.example.onemusic.ui.components.AppleBackwardIcon
import com.example.onemusic.ui.components.AppleForwardIcon
import com.example.onemusic.ui.components.AppleLosslessIcon
import com.example.onemusic.ui.components.ApplePauseIcon
import com.example.onemusic.ui.components.ApplePlayIcon
import com.example.onemusic.ui.components.TrackDetailsDialog
import com.example.onemusic.ui.screens.player.backdrop.NowPlayingBackdrop
import com.example.onemusic.ui.screens.player.controls.NowPlayingActionDock
import com.example.onemusic.ui.screens.player.controls.NowPlayingProgressSection
import com.example.onemusic.ui.screens.player.lyrics.NowPlayingLyricsPane
import com.example.onemusic.ui.screens.player.queue.NowPlayingQueuePane
import com.example.onemusic.ui.screens.player.dialogs.FavoriteToastBanner
import com.example.onemusic.ui.screens.player.dialogs.PlaybackSpeedDialog
import com.example.onemusic.ui.screens.player.dialogs.SleepTimerDialog
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryBody
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor

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
            isSheetFullyVisible = !isDismissing,
            controlsDeckHeight = controlsDeckHeightDp,
            onArtworkLongClick = { showTrackDetailsDialog = true }
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
                            NowPlayingLyricsPane(
                                track = track,
                                listState = lyricsListState,
                                nestedScrollConnection = lyricsNestedScrollConnection,
                                activeLyricIndex = { activeLyricIndex },
                                positionState = positionState,
                                onSeekToLine = { index, seekTime ->
                                    lastLyricsUserScrollTimeMs = 0L
                                    scope.launch {
                                        lyricsListState.animateScrollToItem(index)
                                    }
                                    onSeek(seekTime)
                                }
                            )
                        }
                        NowPlayingCenterView.QUEUE -> {
                            NowPlayingQueuePane(
                                playbackState = playbackState,
                                listState = queueListState,
                                nestedScrollConnection = queueNestedScrollConnection,
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
                                onOpenSpeedMenu = { isSpeedMenuOpen = true },
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onOpenDetails = { showTrackDetailsDialog = true },
                                onOpenOptions = { showOptionsMenu = true }
                            )
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
            PlaybackSpeedDialog(
                currentSpeed = playbackState.playbackSpeed,
                hazeState = nowPlayingHazeState,
                onSelectSpeed = { speed -> onSetPlaybackSpeed?.invoke(speed) },
                onDismiss = { isSpeedMenuOpen = false }
            )
        }

        // LAYER 4: Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                sleepTimerMinutes = playbackState.sleepTimerMinutes,
                sleepTimerRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
                hazeState = nowPlayingHazeState,
                onSetSleepTimer = onSetSleepTimer,
                onSetSleepTimerEndOfTrack = onSetSleepTimerEndOfTrack,
                onCancelSleepTimer = onCancelSleepTimer,
                onDismiss = { showSleepTimerDialog = false }
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
        FavoriteToastBanner(
            message = favoriteToastMessage,
            hazeState = nowPlayingHazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
