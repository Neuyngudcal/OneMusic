package com.example.onemusic.ui.components
import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.minimumInteractiveComponentSize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.onemusic.data.model.Track

import com.example.onemusic.ui.utils.ApexSpringRelease
import com.example.onemusic.ui.utils.apexBounceClick
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.onemusic.theme.LocalHazeState
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.apexFrostedGlass

/**
 * Floating Pill Mini Player (Now Bar)
 * High-Performance Samsung One UI 8.5 Gesture & Motion Controls:
 * - Tap: Instant expansion into Fullscreen NowPlayingSheet
 * - Swipe Up: Expands into Fullscreen NowPlayingSheet
 * - Swipe Left: Skip to Next track
 * - Swipe Right: Skip to Previous track
 * - Robust state reset preventing any drag stuck/lock issues
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowBar(
    track: Track?,
    isPlaying: Boolean,
    onBarClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hapticEngine = com.example.onemusic.haptics.rememberApexHaptics()
    val currentView = androidx.compose.ui.platform.LocalView.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    // Threshold crossing state trackers to fire latch haptic exactly once per stroke
    var hasFiredLatchX by remember { mutableStateOf(false) }
    var hasFiredLatchY by remember { mutableStateOf(false) }

    // Always ensure offsets and scale are completely clean and reset
    LaunchedEffect(track?.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        scale.snapTo(1f)
    }

    fun triggerExpand() {
        scope.launch {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            scale.snapTo(1f)
        }
        onBarClick()
    }

    val hazeState = com.example.onemusic.theme.LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .height(58.dp)
            .shadow(elevation = 16.dp, shape = PillShape, ambientColor = AppTheme.colors.shadow)
            .clip(PillShape)
            .apexFrostedGlass(
                backgroundColor = AppTheme.colors.surface1.copy(alpha = 0.88f),
                blurRadius = 20.dp,
                hazeState = hazeState
            )
            .border(0.85.dp, AppTheme.colors.pillBorderBrush, PillShape)

            .pointerInput(Unit) {
                // Ngưỡng tính bằng dp (quy ra px theo mật độ màn hình) → cảm giác vuốt giống nhau trên mọi máy.
                // Trước đây là số px cứng: máy 3x thì 65px ≈ 22dp (vuốt nhẹ đã chuyển bài), máy mật độ thấp phải vuốt rất xa.
                val skipThresholdPx = 32.dp.toPx()
                val expandThresholdPx = 16.dp.toPx()
                val maxDragXPx = 64.dp.toPx()
                val maxDragYPx = 44.dp.toPx()
                detectDragGestures(
                    onDragStart = {
                        hasFiredLatchX = false
                        hasFiredLatchY = false
                        scope.launch { scale.animateTo(0.97f, animationSpec = ApexSpringRelease) }
                    },
                    onDragEnd = {
                        scope.launch {
                            val dragX = offsetX.value
                            val dragY = offsetY.value
                            // FIX: Trước đây kiểm tra "vuốt lên để mở rộng" trước, không quan tâm
                            // trục nào dịch chuyển nhiều hơn, nên một cú vuốt chéo (vừa ngang vừa
                            // hơi lên) dễ bị hiểu nhầm thành mở rộng thay vì chuyển bài (hoặc ngược
                            // lại). Giờ chỉ xét trục có độ dịch chuyển lớn hơn là trục "thắng".
                            val isVerticalDominant = kotlin.math.abs(dragY) > kotlin.math.abs(dragX)

                            // 1. Swipe Up to expand (chỉ khi trục dọc chiếm ưu thế)
                            if (isVerticalDominant && dragY < -expandThresholdPx) {
                                triggerExpand()
                            }
                            // 2. Swipe Left -> Next Track (chỉ khi trục ngang chiếm ưu thế)
                            else if (!isVerticalDominant && dragX < -skipThresholdPx) {
                                onNextClick()
                            }
                            // 3. Swipe Right -> Previous Track (chỉ khi trục ngang chiếm ưu thế)
                            else if (!isVerticalDominant && dragX > skipThresholdPx) {
                                if (onPreviousClick != null) {
                                    onPreviousClick()
                                } else {
                                    onNextClick()
                                }
                            }

                            // Smooth spring return
                            launch { offsetX.animateTo(0f, animationSpec = ApexSpringRelease) }
                            launch { offsetY.animateTo(0f, animationSpec = ApexSpringRelease) }
                            launch { scale.animateTo(1f, animationSpec = ApexSpringRelease) }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { offsetX.animateTo(0f, animationSpec = ApexSpringRelease) }
                            launch { offsetY.animateTo(0f, animationSpec = ApexSpringRelease) }
                            launch { scale.animateTo(1f, animationSpec = ApexSpringRelease) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = (offsetX.value + dragAmount.x * 0.6f).coerceIn(-maxDragXPx, maxDragXPx)
                        val newY = (offsetY.value + dragAmount.y * 0.6f).coerceIn(-maxDragYPx, 0f)
                        scope.launch {
                            offsetX.snapTo(newX)
                            offsetY.snapTo(newY)
                        }

                        // Real-time Spring Latch Haptic Cue when passing horizontal track skip threshold
                        if (!hasFiredLatchX && kotlin.math.abs(newX) >= skipThresholdPx) {
                            hapticEngine.performSpringLatch(scale = 0.60f, fallbackView = currentView)
                            hasFiredLatchX = true
                        } else if (hasFiredLatchX && kotlin.math.abs(newX) < skipThresholdPx * 0.77f) {
                            hasFiredLatchX = false
                        }

                        // Real-time Spring Latch Haptic Cue when passing vertical expand threshold
                        if (!hasFiredLatchY && newY <= -expandThresholdPx) {
                            hapticEngine.performSpringLatch(scale = 0.65f, fallbackView = currentView)
                            hasFiredLatchY = true
                        } else if (hasFiredLatchY && newY > -expandThresholdPx * 0.7f) {
                            hasFiredLatchY = false
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        scope.launch { scale.animateTo(0.96f, animationSpec = ApexSpringRelease) }
                        tryAwaitRelease()
                        scope.launch { scale.animateTo(1f, animationSpec = ApexSpringRelease) }
                    },
                    onTap = {
                        hapticEngine.performCrispTap(scale = 0.35f, fallbackView = currentView)
                        triggerExpand()
                    }
                )
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Icon Squircle with Smooth Morph Crossfade
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.surface1),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = (track?.id ?: "") to (track?.artworkUrl ?: ""),
                    transitionSpec = {
                        fadeIn(tween(300, easing = FastOutSlowInEasing))
                            .togetherWith(fadeOut(tween(220, easing = FastOutSlowInEasing)))
                    },
                    label = "nowbar_art_morph_anim",
                    modifier = Modifier.fillMaxSize()
                ) { (_, artworkUrl) ->
                    if (artworkUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artworkUrl)
                                .size(160, 160)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = track?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = AppTheme.colors.textPrimary.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info with Marquee
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track?.title ?: "Không phát nhạc",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = track?.artist ?: "Nhấn để phát",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppTheme.colors.textSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Pause Button with Touch Feedback
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize() // vùng chạm ≥ 48dp, hình giữ nguyên
                    .size(38.dp)
                    .clip(PillShape)
                    .apexBounceClick(scaleDown = 0.85f, enableHaptic = true) {
                        onPlayPauseClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}


