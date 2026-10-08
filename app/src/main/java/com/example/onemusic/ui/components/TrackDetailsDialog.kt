package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.AudioMetadataInspector
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.IvoryFaint
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.PillShape

/**
 * Dialog hiển thị chi tiết chất lượng âm thanh theo phong cách Apple Music Lossless Modal
 * - Hero icon Apple Lossless sóng 3 vạch tối giản (chỉ xuất hiện đối với file FLAC chất lượng cao)
 * - Tiêu đề chất lượng (Lossless / Hi-Res Lossless / High Quality) & phụ đề định dạng/bit depth/sample rate
 * - Đường dẫn file lưu trữ nội bộ
 * - Cụm nút thao tác nảy xúc giác lò xo
 */
@Composable
fun TrackDetailsDialog(
    track: Track,
    onDismiss: () -> Unit,
    onPlayNext: ((Track) -> Unit)? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val context = LocalContext.current
    val details = remember(track) {
        AudioMetadataInspector.inspectTrack(context, track)
    }

    val isFlac = details.format.equals("FLAC", ignoreCase = true)
    val sampleRateKhzStr = details.sampleRateHz?.let { sr ->
        if (sr % 1000 == 0) "${sr / 1000} kHz" else "${sr / 1000.0} kHz"
    } ?: "44.1 kHz"

    val qualityTitle = when {
        details.isHiRes -> "Hi-Res Lossless"
        details.isLossless -> "Lossless"
        (details.bitrateKbps ?: 0) >= 320 -> "High Quality"
        else -> details.format
    }

    val qualitySubtitle = when {
        details.isLossless -> {
            val bitDepthStr = "${details.bitDepth ?: 16}-bit"
            "${details.format} $bitDepthStr/$sampleRateKhzStr"
        }
        details.bitrateKbps != null -> {
            "${details.format} ${details.bitrateKbps} kbps" + (if (sampleRateKhzStr.isNotBlank()) " / $sampleRateKhzStr" else "")
        }
        else -> details.format
    }

    ApexDialogContainer(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Apple Music Lossless Quality Display (Clean centered hero)
            if (isFlac) {
                Spacer(modifier = Modifier.height(6.dp))
                AppleLosslessIcon(
                    modifier = Modifier.size(width = 72.dp, height = 46.dp),
                    tint = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = qualityTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 21.sp,
                    letterSpacing = (-0.2).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = qualitySubtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = IvoryMedium,
                    fontSize = 14.sp,
                    letterSpacing = 0.1.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Thẻ đường dẫn tệp (File Path Card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.surface2)
                    .border(0.7.dp, IvorySubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = IvoryFaint,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = details.filePath.ifBlank { "Lưu trữ nội bộ" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IvoryMedium,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cụm nút hành động: Phát kế tiếp & Đóng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onPlayNext != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(PillShape)
                            .background(AppTheme.colors.textPrimary)
                            .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                                onPlayNext(track)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = null,
                                tint = AppTheme.colors.onInverse,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Phát kế tiếp",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.onInverse,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(PillShape)
                        .background(if (onPlayNext != null) AppTheme.colors.surfaceControl else AppTheme.colors.textPrimary)
                        .then(
                            if (onPlayNext != null) Modifier.border(1.5.dp, AppTheme.colors.borderStrong, PillShape) else Modifier
                        )
                        .apexBounceClick(scaleDown = 0.94f, enableHaptic = true) {
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Đóng",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (onPlayNext != null) AppTheme.colors.textPrimary else AppTheme.colors.onInverse,
                            fontSize = 14.5.sp
                        )
                    )
                }
            }
        }
    }
}
