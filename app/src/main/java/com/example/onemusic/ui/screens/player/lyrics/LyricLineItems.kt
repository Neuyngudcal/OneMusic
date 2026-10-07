package com.example.onemusic.ui.screens.player.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.LyricLine
import com.example.onemusic.data.model.LyricWord
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * Kiểm tra xem dòng lời bài hát có phải là đoạn dạo nhạc không lời (chỉ chứa nốt nhạc hoặc ký hiệu dạo)
 */
internal fun isInstrumentalLine(text: String): Boolean {
    val clean = text.trim()
    if (clean.isEmpty()) return false
    val musicNoteChars = setOf('♪', '♫', '♬', '♩', '♭', '♮', '♯', '.', '•', '-', '–', '—', '*', '~', '_')
    val isOnlySymbols = clean.all { it in musicNoteChars || it.isWhitespace() || it == '(' || it == ')' || it == '[' || it == ']' }
    if (isOnlySymbols || clean.contains("🎶") || clean.contains("🎵")) return true

    val lower = clean.lowercase().removeSurrounding("(", ")").removeSurrounding("[", "]").trim()
    return lower in setOf("instrumental", "intro", "music", "solo", "interlude", "outro", "dạo nhạc", "nhạc dạo")
}

@Composable
private fun InstrumentalDotsLyricItem(
    isActive: Boolean,
    itemScale: Float,
    alpha: Float,
    onSeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "instrumental_dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = if (isActive) 1.0f else 1.0f,
        targetValue = if (isActive) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot1_scale"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = if (isActive) 1.0f else 1.0f,
        targetValue = if (isActive) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot2_scale"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = if (isActive) 1.0f else 1.0f,
        targetValue = if (isActive) 1.35f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot3_scale"
    )

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.45f else 0.30f,
        targetValue = if (isActive) 1.00f else 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot1_alpha"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.45f else 0.30f,
        targetValue = if (isActive) 1.00f else 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot2_alpha"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.45f else 0.30f,
        targetValue = if (isActive) 1.00f else 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot3_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = itemScale
                scaleY = itemScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                onSeek()
            }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotProps = listOf(
                Pair(dot1Scale, dot1Alpha),
                Pair(dot2Scale, dot2Alpha),
                Pair(dot3Scale, dot3Alpha)
            )

            for ((scale, dotA) in dotProps) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = if (isActive) scale else 1.0f
                            scaleY = if (isActive) scale else 1.0f
                        }
                        .clip(CircleShape)
                        .background(PrimaryIvory.copy(alpha = if (isActive) dotA else 0.30f))
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordByWordLyricItem(
    lyric: LyricLine,
    isActive: Boolean,
    distanceFromActive: Int = 0,
    // Chỉ đọc .value khi là dòng đang hát → các dòng khác không vẽ lại mỗi 40ms
    positionState: androidx.compose.runtime.State<Long>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyric.text.isBlank() || lyric.text.equals("null", ignoreCase = true) || lyric.text.equals("(null)", ignoreCase = true)) {
        return
    }

    // Unified base size for all lines, preserving Apple Music highlight zoom for active line
    val (targetAlpha, targetScale) = when (distanceFromActive) {
        0 -> Pair(1.0f, 1.06f) // GPU Layer scale zoom for active line
        1 -> Pair(0.58f, 1.00f)
        2 -> Pair(0.38f, 1.00f)
        else -> Pair(0.22f, 1.00f)
    }

    val itemScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "lyric_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "lyric_alpha"
    )

    val isInstrumental = remember(lyric.text) { isInstrumentalLine(lyric.text) }

    if (isInstrumental) {
        // Apple Music Style: Nếu là đoạn dạo nhạc (chỉ có nốt nhạc) -> hiển thị 3 chấm tròn đập nhịp
        InstrumentalDotsLyricItem(
            isActive = isActive,
            itemScale = itemScale,
            alpha = alpha,
            onSeek = { onSeek(lyric.timestampMs) },
            modifier = modifier
        )
    } else if (lyric.words.isNotEmpty()) {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = itemScale
                    scaleY = itemScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                    onSeek(lyric.timestampMs)
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // +60ms bù trễ hiển thị, giống cách tính activeLyricIndex
            val currentPositionMs = if (isActive) positionState.value + 60L else 0L
            for (word in lyric.words) {
                val isWordFinished = isActive && currentPositionMs >= word.endMs
                val isWordActive = isActive && currentPositionMs in word.startMs until word.endMs

                LyricWordChip(
                    word = word,
                    isLineActive = isActive,
                    isWordFinished = isWordFinished,
                    isWordActive = isWordActive,
                    onSeek = onSeek
                )
            }
        }
    } else {
        val textColor by animateColorAsState(
            targetValue = if (isActive) TextPrimary else IvoryHigh,
            animationSpec = tween(180),
            label = "lyric_color"
        )

        val textShadow = remember(isActive) {
            if (isActive) {
                Shadow(
                    color = IvoryFaint,
                    blurRadius = 14f
                )
            } else {
                Shadow.None
            }
        }

        Text(
            text = lyric.text,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                lineHeight = 44.sp,
                shadow = textShadow,
                letterSpacing = (-0.4).sp
            ),
            textAlign = TextAlign.Start,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = itemScale
                    scaleY = itemScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .apexBounceClick(scaleDown = 0.98f, enableHaptic = true) {
                    onSeek(lyric.timestampMs)
                }
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun LyricWordChip(
    word: LyricWord,
    isLineActive: Boolean,
    isWordFinished: Boolean,
    isWordActive: Boolean,
    onSeek: (Long) -> Unit
) {
    val wordScale by animateFloatAsState(
        targetValue = if (isWordActive) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "word_scale"
    )

    val wordColor by animateColorAsState(
        targetValue = when {
            !isLineActive -> IvoryHigh
            isWordFinished || isWordActive -> TextPrimary
            else -> IvoryFaint
        },
        animationSpec = tween(120),
        label = "word_color"
    )

    val wordShadow = remember(isWordActive, isWordFinished) {
        if (isWordActive) {
            Shadow(color = IvoryHigh, blurRadius = 16f)
        } else if (isWordFinished) {
            Shadow(color = IvoryMuted, blurRadius = 6f)
        } else {
            Shadow.None
        }
    }

    Text(
        text = word.text + " ",
        style = MaterialTheme.typography.headlineMedium.copy(
            color = wordColor,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            lineHeight = 44.sp,
            shadow = wordShadow,
            letterSpacing = (-0.4).sp
        ),
        modifier = Modifier
            .graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                onSeek(word.startMs)
            }
    )
}
