package com.example.onemusic.ui.screens.player

import android.content.Context
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.TextureView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextDisabled
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

/**
 * Hiển thị ảnh bìa album tĩnh kèm mặt nạ dải mờ chuyển sắc xuống nền
 */
@Composable
fun StaticAlbumArtwork(
    context: Context,
    track: Track?,
    applyMask: Boolean = true,
    scrimColor: Color = Color.Transparent
) {
    if (track != null && track.artworkUrl.isNotBlank()) {
        val imageRequest = remember(track.artworkUrl) {
            ImageRequest.Builder(context)
                .data(track.artworkUrl)
                .crossfade(150)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }
        val maskModifier = if (applyMask) {
            Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val targetH = size.height
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Black,
                            0.28f to Color.Black,
                            0.42f to Color.Black.copy(alpha = 0.96f),
                            0.54f to Color.Black.copy(alpha = 0.88f),
                            0.66f to Color.Black.copy(alpha = 0.74f),
                            0.76f to Color.Black.copy(alpha = 0.55f),
                            0.85f to Color.Black.copy(alpha = 0.35f),
                            0.92f to Color.Black.copy(alpha = 0.18f),
                            0.97f to Color.Black.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            startY = 0f,
                            endY = targetH
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(maskModifier)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )

            // Lớp sương mù hòa sắc (Palette-Tinted Scrim Fog) nhuộm nhẹ chân ảnh hòa tan vào Ambient Mesh
            if (applyMask && scrimColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.30f to Color.Transparent,
                                0.52f to scrimColor.copy(alpha = 0.20f),
                                0.70f to scrimColor.copy(alpha = 0.55f),
                                0.86f to scrimColor.copy(alpha = 0.85f),
                                1.00f to scrimColor
                            )
                        )
                )
            }
        }
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

/**
 * Motion Artwork Player with CenterCrop Matrix Precision (Apple Music animated album art)
 */
@Composable
fun MotionArtworkPlayer(
    motionVideoPath: String,
    motionPlayer: ExoPlayer?,
    track: Track?,
    hazeState: HazeState,
    isSheetFullyVisible: Boolean,
    scrimColor: Color = Color.Transparent
) {
    val context = LocalContext.current
    val isNetworkStream = motionVideoPath.startsWith("http://") || motionVideoPath.startsWith("https://")
    val videoFile = remember(motionVideoPath) { if (!isNetworkStream) File(motionVideoPath) else null }

    val videoInfo by produceState<Pair<Int, Int>?>(initialValue = null, motionVideoPath) {
        value = if (videoFile == null) {
            Pair(1080, 1080)
        } else {
            withContext(Dispatchers.IO) {
                if (!videoFile.exists() || videoFile.length() == 0L) {
                    Pair(-1, -1)
                } else {
                    runCatching {
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(videoFile.absolutePath)
                            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                            val rotation = rotationStr?.toIntOrNull() ?: 0
                            var w = widthStr?.toIntOrNull() ?: 0
                            var h = heightStr?.toIntOrNull() ?: 0
                            if (rotation == 90 || rotation == 270) {
                                val temp = w
                                w = h
                                h = temp
                            }
                            Pair(w, h)
                        }
                    }.getOrDefault(Pair(0, 0))
                }
            }
        }
    }

    val videoDimensions = videoInfo
    if (videoDimensions == null || videoDimensions.first < 0) {
        StaticAlbumArtwork(context, track, applyMask = true, scrimColor = scrimColor)
        return
    }

    val fallbackPlayer = if (motionPlayer == null) {
        remember(motionVideoPath) {
            val motionLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 30_000,
                    /* bufferForPlaybackMs = */ 0,
                    /* bufferForPlaybackAfterRebufferMs = */ 0
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioRenderers(
                    context: Context,
                    extensionRendererMode: Int,
                    mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                    enableDecoderFallback: Boolean,
                    audioSink: androidx.media3.exoplayer.audio.AudioSink,
                    eventHandler: android.os.Handler,
                    eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                    out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
                ) {}
            }

            ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(motionLoadControl)
                .build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                    playWhenReady = true
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                seekTo(0L)
                                playWhenReady = true
                            }
                        }
                    })
                    val uri = if (isNetworkStream) Uri.parse(motionVideoPath) else Uri.fromFile(videoFile)
                    val item = if (motionVideoPath.contains(".m3u8")) {
                        MediaItem.Builder()
                            .setUri(uri)
                            .setMimeType(MimeTypes.APPLICATION_M3U8)
                            .build()
                    } else {
                        MediaItem.fromUri(uri)
                    }
                    setMediaItem(item)
                    prepare()
                }
        }
    } else null

    val activePlayer = motionPlayer ?: fallbackPlayer
    val registeredSizeListenerHolder = remember { arrayOfNulls<Player.Listener>(1) }

    DisposableEffect(activePlayer, motionVideoPath) {
        onDispose {
            registeredSizeListenerHolder[0]?.let { listener ->
                activePlayer?.removeListener(listener)
            }
            registeredSizeListenerHolder[0] = null
            fallbackPlayer?.release()
        }
    }

    LaunchedEffect(isSheetFullyVisible, activePlayer) {
        activePlayer?.playWhenReady = isSheetFullyVisible
    }

    var isFirstFrameRendered by remember(motionVideoPath, track?.id) { mutableStateOf(false) }

    val motionVideoAlpha by animateFloatAsState(
        targetValue = if (isFirstFrameRendered) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "motion_artwork_awakening"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val targetH = size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color.Black,
                        0.28f to Color.Black,
                        0.42f to Color.Black.copy(alpha = 0.96f),
                        0.54f to Color.Black.copy(alpha = 0.88f),
                        0.66f to Color.Black.copy(alpha = 0.74f),
                        0.76f to Color.Black.copy(alpha = 0.55f),
                        0.85f to Color.Black.copy(alpha = 0.35f),
                        0.92f to Color.Black.copy(alpha = 0.18f),
                        0.97f to Color.Black.copy(alpha = 0.05f),
                        1.00f to Color.Transparent,
                        startY = 0f,
                        endY = targetH
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        StaticAlbumArtwork(context, track, applyMask = false)

        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    alpha = 0f

                    var lastVWidth = if (videoDimensions.first > 0) videoDimensions.first else (activePlayer?.videoSize?.width ?: 0)
                    var lastVHeight = if (videoDimensions.second > 0) videoDimensions.second else (activePlayer?.videoSize?.height ?: 0)

                    fun updateMatrix(vW: Int, vH: Int) {
                        val w = width.toFloat()
                        val h = height.toFloat()
                        if (w <= 0f || h <= 0f) return

                        val videoW = if (vW > 0) vW.toFloat() else (activePlayer?.videoSize?.width?.takeIf { it > 0 }?.toFloat() ?: (videoDimensions.first.takeIf { it > 0 }?.toFloat() ?: 1080f))
                        val videoH = if (vH > 0) vH.toFloat() else (activePlayer?.videoSize?.height?.takeIf { it > 0 }?.toFloat() ?: (videoDimensions.second.takeIf { it > 0 }?.toFloat() ?: 1080f))
                        if (videoW <= 0f || videoH <= 0f) return

                        val scale = maxOf(w / videoW, h / videoH)
                        val targetW = videoW * scale
                        val targetH = videoH * scale

                        val matrixScaleX = targetW / w
                        val matrixScaleY = targetH / h

                        val matrix = Matrix()
                        matrix.setScale(matrixScaleX, matrixScaleY, w / 2f, h / 2f)
                        setTransform(matrix)
                    }

                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                    }

                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }

                    val sizeListener = object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            if (videoSize.width > 0 && videoSize.height > 0) {
                                lastVWidth = videoSize.width
                                lastVHeight = videoSize.height
                                post { updateMatrix(lastVWidth, lastVHeight) }
                            }
                        }

                        override fun onRenderedFirstFrame() {
                            post {
                                updateMatrix(lastVWidth, lastVHeight)
                                isFirstFrameRendered = true
                            }
                        }
                    }

                    activePlayer?.addListener(sizeListener)
                    registeredSizeListenerHolder[0] = sizeListener
                    activePlayer?.setVideoTextureView(this)
                    post { updateMatrix(lastVWidth, lastVHeight) }
                }
            },
            update = { view ->
                view.alpha = motionVideoAlpha
                view.post {
                    val w = view.width.toFloat()
                    val h = view.height.toFloat()
                    if (w > 0f && h > 0f) {
                        val videoW = activePlayer?.videoSize?.width?.takeIf { it > 0 }?.toFloat() ?: (videoDimensions.first.takeIf { it > 0 }?.toFloat() ?: 1080f)
                        val videoH = activePlayer?.videoSize?.height?.takeIf { it > 0 }?.toFloat() ?: (videoDimensions.second.takeIf { it > 0 }?.toFloat() ?: 1080f)
                        if (videoW > 0f && videoH > 0f) {
                            val scale = maxOf(w / videoW, h / videoH)
                            val targetW = videoW * scale
                            val targetH = videoH * scale
                            val matrixScaleX = targetW / w
                            val matrixScaleY = targetH / h
                            val matrix = Matrix()
                            matrix.setScale(matrixScaleX, matrixScaleY, w / 2f, h / 2f)
                            view.setTransform(matrix)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = motionVideoAlpha }
        )

        if (scrimColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.30f to Color.Transparent,
                            0.52f to scrimColor.copy(alpha = 0.20f),
                            0.70f to scrimColor.copy(alpha = 0.55f),
                            0.86f to scrimColor.copy(alpha = 0.85f),
                            1.00f to scrimColor
                        )
                    )
            )
        }
    }
}

/**
 * Layer 1A: Hero Album Artwork Carousel (Horizontal Pager chuyển đổi bìa đĩa giữa các bài)
 */
@Composable
fun NowPlayingArtworkCarousel(
    pagerState: PagerState,
    queue: List<Track>,
    track: Track?,
    motionVideoPath: String?,
    motionPlayer: ExoPlayer?,
    artworkScale: Float,
    isDismissing: Boolean,
    hazeState: HazeState,
    appSettings: AppSettings?,
    animatedSecondaryColor: Color,
    controlsDeckHeightDp: Dp,
    onLongClick: (Track?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                                onLongClick = { onLongClick(currentTrack) }
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
                                hazeState = hazeState,
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

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(controlsDeckHeightDp)
        )
    }
}
