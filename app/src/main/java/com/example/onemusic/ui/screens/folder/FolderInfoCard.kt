package com.example.onemusic.ui.screens.folder

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.ApexAmber
import com.example.onemusic.theme.IvorySubtle
import com.example.onemusic.theme.apexGlassCard

/** Item 4: thẻ "THÔNG TIN BỘ NHỚ & ĐỊNH DẠNG" (tổng số bài, định dạng hỗ trợ). */
@Composable
internal fun FolderInfoCard(totalTrackCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "THÔNG TIN BỘ NHỚ & ĐỊNH DẠNG",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .apexGlassCard(shape = RoundedCornerShape(26.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Tổng số bài hát
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AudioFile,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Tổng số bài hát khả dụng",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$totalTrackCount bài",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp, end = 18.dp),
                    thickness = 0.5.dp,
                    color = IvorySubtle
                )

                // Row 2: Định dạng hỗ trợ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SdCard,
                        contentDescription = null,
                        tint = ApexAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Định dạng giải mã",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "FLAC 24-bit, WAV 192kHz, DSD, ALAC, MP3, AAC, M4A",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
