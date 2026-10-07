package com.example.onemusic.ui.screens.player.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.ScrimColor
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.utils.formatDuration
import com.example.onemusic.ui.utils.formatRemaining
import kotlinx.coroutines.delay

/**
 * Thanh tua + nhãn thời gian đã phát / còn lại của Now Playing.
 * Là composable DUY NHẤT đọc positionState.value, nên mỗi 40ms chỉ phần này vẽ lại thay vì cả NowPlayingSheet.
 * Trạng thái kéo tua cũng nằm ở đây để việc kéo không làm vẽ lại sheet cha.
 */
@Composable
internal fun NowPlayingProgressSection(
    positionState: androidx.compose.runtime.State<Long>,
    totalDurMs: Long,
    hasTrack: Boolean,
    onSeek: (Long) -> Unit,
    centerBadge: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    // Giữ vị trí vừa tua trong chốc lát để thanh tua không giật lùi trước khi player cập nhật vị trí thật
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
