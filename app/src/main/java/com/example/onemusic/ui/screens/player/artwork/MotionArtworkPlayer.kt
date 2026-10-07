package com.example.onemusic.ui.screens.player.artwork

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.onemusic.data.model.Track
import dev.chrisbanes.haze.HazeState

/**
 * Motion Artwork Player with CenterCrop Matrix Precision
 */
@Composable
internal fun MotionArtworkPlayer(
    motionVideoPath: String,
    motionPlayer: androidx.media3.exoplayer.ExoPlayer?,
    track: Track?,
    hazeState: HazeState,
    isSheetFullyVisible: Boolean,
    scrimColor: Color = Color.Transparent
) {
    val context = LocalContext.current
    val isNetworkStream = motionVideoPath.startsWith("http://") || motionVideoPath.startsWith("https://")
    val videoFile = remember(motionVideoPath) { if (!isNetworkStream) java.io.File(motionVideoPath) else null }

    // File.exists() và MediaMetadataRetriever là I/O → chạy trên luồng IO thay vì luồng giao diện.
    // null = đang kiểm tra; Pair = file hợp lệ + kích thước video; (-1,-1) = file không dùng được.
    val videoInfo by androidx.compose.runtime.produceState<Pair<Int, Int>?>(initialValue = null, motionVideoPath) {
        value = if (videoFile == null) {
            Pair(1080, 1080)
        } else {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (!videoFile.exists() || videoFile.length() == 0L) {
                    Pair(-1, -1)
                } else {
                    runCatching {
                        android.media.MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(videoFile.absolutePath)
                            val widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            val rotationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
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

    // Đang kiểm tra file hoặc file không hợp lệ → hiển thị ảnh bìa tĩnh
    val videoDimensions = videoInfo
    if (videoDimensions == null || videoDimensions.first < 0) {
        StaticAlbumArtwork(context, track, applyMask = true, scrimColor = scrimColor)
        return
    }

    val fallbackPlayer = if (motionPlayer == null) {
        remember(motionVideoPath) {
            val motionLoadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 30_000,
                    /* bufferForPlaybackMs = */ 0,
                    /* bufferForPlaybackAfterRebufferMs = */ 0
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
                override fun buildAudioRenderers(
                    context: android.content.Context,
                    extensionRendererMode: Int,
                    mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector,
                    enableDecoderFallback: Boolean,
                    audioSink: androidx.media3.exoplayer.audio.AudioSink,
                    eventHandler: android.os.Handler,
                    eventListener: androidx.media3.exoplayer.audio.AudioRendererEventListener,
                    out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
                ) {}
            }

            androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
                .setLoadControl(motionLoadControl)
                .build().apply {
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                    volume = 0f
                    playWhenReady = true
                    trackSelectionParameters = trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                        .build()
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == androidx.media3.common.Player.STATE_ENDED) {
                                seekTo(0L)
                                playWhenReady = true
                            }
                        }
                    })
                    val uri = if (isNetworkStream) android.net.Uri.parse(motionVideoPath) else android.net.Uri.fromFile(videoFile)
                    val item = if (motionVideoPath.contains(".m3u8")) {
                        androidx.media3.common.MediaItem.Builder()
                            .setUri(uri)
                            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                            .build()
                    } else {
                        androidx.media3.common.MediaItem.fromUri(uri)
                    }
                    setMediaItem(item)
                    prepare()
                }
        }
    } else null

    val activePlayer = motionPlayer ?: fallbackPlayer

    // FIX: Giữ tham chiếu tới Player.Listener (sizeListener) đã đăng ký trên activePlayer
    // để có thể gỡ bỏ đúng cách khi composable rời khỏi màn hình. Trước đây listener này
    // KHÔNG BAO GIỜ được removeListener, nên mỗi lần AndroidView tạo TextureView mới (đổi bài
    // có motion cover, dùng chung 1 ExoPlayer từ ngoài truyền vào) sẽ rò rỉ thêm 1 listener
    // giữ tham chiếu tới TextureView cũ, vi phạm quy tắc Zero-Leak Lifecycle của dự án.
    val registeredSizeListenerHolder = remember { arrayOfNulls<androidx.media3.common.Player.Listener>(1) }

    androidx.compose.runtime.DisposableEffect(activePlayer, motionVideoPath) {
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

    // Hiệu ứng "Bìa Đĩa Sống Dậy": Theo dõi frame đầu tiên được giải mã để kích hoạt hòa tan mượt mà
    var isFirstFrameRendered by remember(motionVideoPath, track?.id) { mutableStateOf(false) }

    val motionVideoAlpha by animateFloatAsState(
        targetValue = if (isFirstFrameRendered) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "motion_artwork_awakening"
    )

    val vW = if (videoDimensions.first > 0) videoDimensions.first.toFloat() else (activePlayer?.videoSize?.width?.takeIf { it > 0 }?.toFloat() ?: 1080f)
    val vH = if (videoDimensions.second > 0) videoDimensions.second.toFloat() else (activePlayer?.videoSize?.height?.takeIf { it > 0 }?.toFloat() ?: 1080f)

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
        // 1. Ảnh tĩnh luôn hiển thị sẵn sàng bên dưới làm nền tảng vững chắc (0ms)
        // applyMask = false để Box cha quản lý mặt nạ quang học duy nhất, tránh méo alpha
        StaticAlbumArtwork(context, track, applyMask = false)

        // 2. Motion Video TextureView phủ phía trên, xuất hiện dần qua hoạt ảnh thức tỉnh
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
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

                        // CenterCrop Matrix Scaling 1:1: Hoàn toàn khít vuông, không biến dạng, căn giữa hoàn hảo
                        val scale = maxOf(w / videoW, h / videoH)
                        val targetW = videoW * scale
                        val targetH = videoH * scale

                        val matrixScaleX = targetW / w
                        val matrixScaleY = targetH / h

                        val matrix = android.graphics.Matrix()
                        matrix.setScale(matrixScaleX, matrixScaleY, w / 2f, h / 2f)
                        setTransform(matrix)
                    }

                    addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                    }

                    surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                            updateMatrix(lastVWidth, lastVHeight)
                        }
                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = true
                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                    }

                    val sizeListener = object : androidx.media3.common.Player.Listener {
                        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
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
                    // FIX: lưu lại listener vừa đăng ký để DisposableEffect ở trên có thể
                    // removeListener khi composable này bị hủy (đổi bài / rời trang Pager).
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
                            val matrix = android.graphics.Matrix()
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

        // 3. Lớp sương mù hòa sắc (Palette-Tinted Scrim Fog) nhuộm nhẹ chân video hòa tan vào Ambient Mesh
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
