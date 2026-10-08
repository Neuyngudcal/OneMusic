package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.onemusic.haptics.rememberApexHaptics
import com.example.onemusic.theme.PillShape

val ALPHABET_CHAR_LIST = listOf(
    '#', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
    'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
    'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
)

/**
 * OneMusic Apex Prism Alphabet Fast Scroller Rail with Center Letter Bubble.
 * Allows instant 120Hz scrubbing through large music libraries with tactile Haptics.
 */
@Composable
fun ApexAlphabetScroller(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
    availableLetters: Set<Char> = emptySet()
) {
    val hapticEngine = rememberApexHaptics()
    val currentView = LocalView.current

    var isScrubbing by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var containerHeightPx by remember { mutableStateOf(1f) }
    var lastLetterChangeTime by remember { mutableStateOf(0L) }

    fun processTouchY(y: Float, isInitial: Boolean = false) {
        if (containerHeightPx <= 0) return
        val clampedY = y.coerceIn(0f, containerHeightPx - 1f)
        val index = ((clampedY / containerHeightPx) * ALPHABET_CHAR_LIST.size).toInt().coerceIn(0, ALPHABET_CHAR_LIST.size - 1)
        val selectedChar = ALPHABET_CHAR_LIST[index]
        if (selectedChar != activeLetter || isInitial) {
            val now = SystemClock.uptimeMillis()
            val timeDelta = if (!isInitial && lastLetterChangeTime > 0L) {
                (now - lastLetterChangeTime).coerceIn(10L, 300L)
            } else {
                200L
            }
            lastLetterChangeTime = now
            activeLetter = selectedChar

            // Adaptive Velocity Haptics:
            // Fast scrub (timeDelta <= 30ms): scale = 0.12f (micro ratchet ticks)
            // Slow/deliberate scrub (timeDelta >= 140ms) or initial touch: scale = 0.25f (solid crisp tooth ticks)
            val speedFraction = ((timeDelta - 30f) / 110f).coerceIn(0f, 1f)
            val adaptiveScale = lerp(0.12f, 0.25f, speedFraction)

            try {
                hapticEngine.performGearTick(scale = adaptiveScale, fallbackView = currentView)
            } catch (_: Exception) {}
            onLetterSelected(selectedChar)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        // 1. Slender Vertical Alphabet Rail Pill
        Box(
            modifier = Modifier
                .width(18.dp)
                .fillMaxHeight()
                .clip(PillShape)
                .background(if (isScrubbing) AppTheme.colors.surface2.copy(alpha = 0.85f) else Color.Transparent)
                .onSizeChanged { containerHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isScrubbing = true
                            processTouchY(offset.y, isInitial = true)
                            tryAwaitRelease()
                            isScrubbing = false
                            activeLetter = null
                            lastLetterChangeTime = 0L
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            processTouchY(offset.y, isInitial = true)
                        },
                        onDragEnd = {
                            isScrubbing = false
                            activeLetter = null
                            lastLetterChangeTime = 0L
                        },
                        onDragCancel = {
                            isScrubbing = false
                            activeLetter = null
                            lastLetterChangeTime = 0L
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            processTouchY(change.position.y, isInitial = false)
                        }
                    )
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ALPHABET_CHAR_LIST.forEach { char ->
                    val isCharActive = char == activeLetter
                    val hasTracks = availableLetters.isEmpty() || availableLetters.contains(char)

                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isCharActive) 10.sp else 8.sp,
                            fontWeight = if (isCharActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isCharActive -> AppTheme.colors.accent
                                hasTracks -> AppTheme.colors.medium
                                else -> AppTheme.colors.stroke
                            }
                        )
                    )
                }
            }
        }

        // 2. Floating Center Letter Bubble (Large Glass Squircle in screen center)
        AnimatedVisibility(
            visible = isScrubbing && activeLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.80f),
            exit = fadeOut() + scaleOut(targetScale = 0.80f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 64.dp)
                    .size(72.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), ambientColor = AppTheme.colors.shadow)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.colors.surface1)
                    .border(1.dp, AppTheme.colors.accent.copy(alpha = 0.50f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter?.toString() ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 34.sp
                    )
                )
            }
        }
    }
}


