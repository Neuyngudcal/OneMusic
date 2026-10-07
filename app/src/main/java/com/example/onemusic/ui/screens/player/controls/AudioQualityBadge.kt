package com.example.onemusic.ui.screens.player.controls

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.theme.IvoryHigh
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.components.AppleLosslessIcon
import com.example.onemusic.ui.utils.apexBounceClick

/** Nhãn chất lượng âm thanh của một bài (vd "Hi-Res Lossless", "MP3") và có hiện biểu tượng Lossless hay không. */
@Immutable
internal data class AudioQualityInfo(
    val badgeText: String,
    val showLosslessIcon: Boolean
)

/**
 * Nhận dạng định dạng / chất lượng của [track] từ đuôi file, bitRate và (với content://) MIME type.
 * MIME type đọc trên luồng IO; khi đọc xong giá trị trả về có thể đổi (FLAC).
 */
@Composable
internal fun rememberAudioQualityInfo(track: Track?): AudioQualityInfo {
    val context = LocalContext.current

    // Lossless / Hi-Res Audio Tech Badge (Apple Music Precision Frosted capsule under scrubber)
    val isFlacByName = remember(track) {
    track?.let { trk ->
        val urlLower = trk.audioUrl.lowercase()
        urlLower.endsWith(".flac") ||
        urlLower.contains(".flac?") ||
        urlLower.contains(".flac/") ||
        trk.bitRate.contains("flac", ignoreCase = true)
    } ?: false
    }
    // contentResolver.getType là lệnh gọi hệ thống → chạy trên luồng IO thay vì luồng giao diện
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

    // isFlacTrack có thể đổi sau khi produceState chạy xong → phải nằm trong key
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

    // Biểu tượng Lossless chỉ hiển thị độc quyền cho file FLAC chất lượng cao
    val hasAudioBadgeIcon = isFlacTrack

    val audioBadgeText = when {
        isHiResTrack -> "Hi-Res Lossless"
        isLosslessTrack -> "Lossless"
        isHighQualityTrack -> "High Quality"
        trackFormat.isNotBlank() -> trackFormat
        else -> "Lossless"
    }

    return AudioQualityInfo(badgeText = audioBadgeText, showLosslessIcon = hasAudioBadgeIcon)
}

/**
 * Viên nhãn chất lượng âm thanh dưới thanh tua (vd "Lossless" kèm biểu tượng). Bấm → [onClick] (mở "Thông tin bài hát").
 * Đặt vị trí bằng [modifier] (Now Playing truyền `Modifier.align(Alignment.Center)`).
 */
@Composable
internal fun AudioQualityBadge(
    visible: Boolean,
    info: AudioQualityInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
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
                    horizontal = if (info.showLosslessIcon) 9.dp else 11.dp,
                    vertical = 2.5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (info.showLosslessIcon) {
                    AppleLosslessIcon(
                        modifier = Modifier.size(width = 16.dp, height = 10.5.dp),
                        tint = IvoryHigh
                    )
                }
                Text(
                    text = info.badgeText,
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
