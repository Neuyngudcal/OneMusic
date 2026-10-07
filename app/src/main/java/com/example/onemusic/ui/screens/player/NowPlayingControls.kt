package com.example.onemusic.ui.screens.player

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.AppSettings
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.data.model.Track
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.IvoryBody
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.components.AppleBackwardIcon
import com.example.onemusic.ui.components.AppleForwardIcon
import com.example.onemusic.ui.components.AppleLosslessIcon
import com.example.onemusic.ui.components.ApplePauseIcon
import com.example.onemusic.ui.components.ApplePlayIcon
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlin.math.abs

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun formatRemaining(currentMs: Long, totalMs: Long): String {
    if (totalMs <= 0) return "--:--"
    val remainingMs = (totalMs - currentMs).coerceAtLeast(0)
    val totalSeconds = (remainingMs / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("-%d:%02d", minutes, seconds)
}

enum class NowPlayingCenterView {
    ARTWORK,
    LYRICS,
    QUEUE
}

/**
 * Capsule Slider with 120Hz Spring Dynamic Expansion on Touch/Drag
 */
@Composable
fun CapsuleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    height: Dp = 7.dp,
    activeColor: Color = TextPrimary,
    inactiveColor: Color = IvoryMuted
) {
    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current
    var isInteracting by remember { mutableStateOf(false) }
    var lastHapticFraction by remember { mutableFloatStateOf(value) }

    val animatedHeight by animateDpAsState(
        targetValue = if (isInteracting) height * 1.75f else height,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "capsule_slider_height"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isInteracting = true
                        val widthPx = size.width.toFloat()
                        if (widthPx > 0f) {
                            val newFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                            onValueChange(newFraction)
                            hapticEngine.performGearTick(fallbackView = currentView)
                            lastHapticFraction = newFraction
                        }
                        val success = tryAwaitRelease()
                        isInteracting = false
                        if (success) {
                            onValueChangeFinished?.invoke()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        val widthPx = size.width.toFloat()
                        if (widthPx > 0f) {
                            val newFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                            onValueChange(newFraction)
                            hapticEngine.performGearTick(fallbackView = currentView)
                            lastHapticFraction = newFraction
                        }
                    },
                    onDragEnd = {
                        isInteracting = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isInteracting = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat()
                        if (widthPx > 0f) {
                            val newFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                            onValueChange(newFraction)

                            if (abs(newFraction - lastHapticFraction) >= 0.05f) {
                                currentView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                lastHapticFraction = newFraction
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val clampedProgress = value.coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(CircleShape)
                .background(inactiveColor)
        ) {
            Box(
                modifier = Modifier
                    .width(totalWidth * clampedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}

/**
 * Hiệu năng tối ưu: Thanh tua tiến trình và nhãn thời gian
 */
@Composable
fun NowPlayingProgressSection(
    positionState: State<Long>,
    totalDurMs: Long,
    hasTrack: Boolean,
    onSeek: (Long) -> Unit,
    centerBadge: @Composable BoxScope.() -> Unit = {}
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(pendingSeekFraction) {
        if (pendingSeekFraction != null) {
            delay(300L)
            pendingSeekFraction = null
        }
    }

    val currentPositionMs = positionState.value
    val totalDur = totalDurMs.toFloat()
    val rawSliderValue = if (totalDur > 0) (currentPositionMs / totalDur).coerceIn(0f, 1f) else 0f
    val sliderValue = when {
        isScrubbing -> scrubFraction
        pendingSeekFraction != null -> pendingSeekFraction!!
        else -> rawSliderValue
    }

    val displayCurrentTimeMs = if (isScrubbing) (scrubFraction * totalDur).toLong() else currentPositionMs
    val displayRemainingTimeMs = if (isScrubbing) (scrubFraction * totalDur).toLong() else currentPositionMs

    val timeTextShadow = remember {
        Shadow(color = ScrimColor, blurRadius = 8f)
    }

    CapsuleSlider(
        value = sliderValue,
        onValueChange = { frac ->
            isScrubbing = true
            scrubFraction = frac
        },
        onValueChangeFinished = {
            if (totalDur > 0) {
                onSeek((scrubFraction * totalDur).toLong())
                pendingSeekFraction = scrubFraction
            }
            isScrubbing = false
        },
        height = 8.dp,
        activeColor = TextPrimary,
        inactiveColor = IvoryMuted
    )

    Spacer(modifier = Modifier.height(10.dp))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasTrack) formatDuration(displayCurrentTimeMs) else "0:00",
            modifier = Modifier.align(Alignment.CenterStart),
            style = MaterialTheme.typography.labelSmall.copy(
                color = IvoryHigh,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                shadow = timeTextShadow
            )
        )

        centerBadge()

        Text(
            text = if (hasTrack) formatRemaining(displayRemainingTimeMs, totalDurMs) else "--:--",
            modifier = Modifier.align(Alignment.CenterEnd),
            style = MaterialTheme.typography.labelSmall.copy(
                color = IvoryHigh,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                shadow = timeTextShadow
            )
        )
    }
}

/**
 * Bottom 3 Utility Actions (1:1 Apple Music - Standalone floating icons without dark pill capsule):
 * - Left: Lyrics (♫)
 * - Center: Audio Output / Equalizer DSP
 * - Right: Playing Queue (3-bar queue icon)
 */
@Composable
fun NowPlayingActionDock(
    centerView: NowPlayingCenterView,
    isEqualizerOpen: Boolean,
    onSelectLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSelectQueue: () -> Unit,
    onToggleBackToArtwork: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 0: Lyrics
        val isLyricsSelected = centerView == NowPlayingCenterView.LYRICS
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isLyricsSelected) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    if (isLyricsSelected) {
                        onToggleBackToArtwork()
                    } else {
                        onSelectLyrics()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = "Lời bài hát",
                tint = if (isLyricsSelected) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }

        // Tab 1: Audio Output / Equalizer DSP / Cast
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isEqualizerOpen) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    onOpenEqualizer()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Bộ chỉnh âm & DSP",
                tint = if (isEqualizerOpen) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }

        // Tab 2: Queue
        val isQueueSelected = centerView == NowPlayingCenterView.QUEUE
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isQueueSelected) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    if (isQueueSelected) {
                        onToggleBackToArtwork()
                    } else {
                        onSelectQueue()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Hàng đợi phát",
                tint = if (isQueueSelected) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Hàng thông tin bài hát: Tiêu đề + Ca sĩ chạy Marquee, Nút Yêu thích (Đảo màu) và Nút More Options (3 chấm)
 */
@Composable
fun NowPlayingTrackInfoRow(
    track: Track?,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val legibilityTextShadow = remember {
        Shadow(color = ScrimColor, blurRadius = 8f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track?.title ?: "Không phát",
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2500,
                        initialDelayMillis = 2000,
                        spacing = MarqueeSpacing(64.dp),
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
                text = track?.artist ?: "—",
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 2500,
                    initialDelayMillis = 2000,
                    spacing = MarqueeSpacing(64.dp),
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCurrentTrackFav = track?.isFavorite == true
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentTrackFav) PrimaryIvory else IvoryStroke)
                    .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                        track?.let { trk ->
                            onToggleFavorite(trk.id, isCurrentTrackFav)
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

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(IvoryStroke)
                    .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                        onOptionsClick()
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
}

/**
 * Cụm 3 phím điều khiển chính: Previous, Play/Pause, Next (Apple Music Precision)
 */
@Composable
fun NowPlayingMasterControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    buttonThrottleMs: Long = 280L
) {
    var lastButtonSkipTimeMs by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 52.dp)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    val now = SystemClock.elapsedRealtime()
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
            if (isPlaying) {
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
                    val now = SystemClock.elapsedRealtime()
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
}

/**
 * Huy hiệu định dạng âm thanh (Lossless / Hi-Res Audio Tech Badge - 1:1 Apple Music)
 */
@Composable
fun NowPlayingAudioBadge(
    track: Track?,
    appSettings: AppSettings?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val isFlacByName = remember(track) {
        track?.let { trk ->
            val urlLower = trk.audioUrl.lowercase()
            urlLower.endsWith(".flac") ||
            urlLower.contains(".flac?") ||
            urlLower.contains(".flac/") ||
            trk.bitRate.contains("flac", ignoreCase = true)
        } ?: false
    }

    val isFlacByMime by androidx.compose.runtime.produceState(initialValue = false, track) {
        val trk = track
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

    val trackFormat = remember(track, isFlacTrack) {
        track?.let { trk ->
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

    val isHiResTrack = track?.let { trk ->
        trk.isHiRes ||
        trk.bitRate.contains("hi-res", ignoreCase = true) ||
        trk.bitRate.contains("24-bit", ignoreCase = true) ||
        trk.bitRate.contains("96khz", ignoreCase = true) ||
        trk.bitRate.contains("192khz", ignoreCase = true)
    } ?: false

    val isLosslessTrack = isFlacTrack || trackFormat in setOf("WAV", "ALAC", "AIFF", "DSD") || (track?.bitRate?.contains("lossless", ignoreCase = true) == true)
    val isHighQualityTrack = track?.let { trk ->
        trk.bitRate.contains("320", ignoreCase = true) ||
        trk.bitRate.contains("256", ignoreCase = true)
    } ?: false

    val hasAudioBadgeIcon = isFlacTrack

    val audioBadgeText = when {
        isHiResTrack -> "Hi-Res Lossless"
        isLosslessTrack -> "Lossless"
        isHighQualityTrack -> "High Quality"
        trackFormat.isNotBlank() -> trackFormat
        else -> "Lossless"
    }

    val showAudioBadge = (track != null) && (appSettings?.isHiResBadgeEnabled != false)

    AnimatedVisibility(
        visible = showAudioBadge,
        modifier = modifier,
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
                    onClick = onClick
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

