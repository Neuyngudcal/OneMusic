package com.example.onemusic.ui.screens.player.dialogs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.ui.utils.formatDuration
import dev.chrisbanes.haze.HazeState

/**
 * Dialog hẹn giờ tắt nhạc của Now Playing.
 * [sleepTimerMinutes]: null = chưa hẹn, -1 = tắt sau khi hết bài, còn lại = số phút.
 * [sleepTimerRemainingSeconds]: thời gian còn lại (âm = chờ hết bài), null = chưa hẹn.
 */
@Composable
internal fun SleepTimerDialog(
    sleepTimerMinutes: Int?,
    sleepTimerRemainingSeconds: Long?,
    hazeState: HazeState,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    ApexDialogContainer(
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Hẹn giờ tắt nhạc",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 20.sp
                )
            )

            if (sleepTimerRemainingSeconds != null) {
                val rem = sleepTimerRemainingSeconds
                val display = if (rem < 0) "Sau khi kết thúc bài hát" else formatDuration(rem * 1000, padMinutes = true)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Đang chạy: $display",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Brand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val timerOptions = listOf(
                15 to "15 phút",
                30 to "30 phút",
                45 to "45 phút",
                60 to "60 phút",
                -1 to "Sau khi kết thúc bài hát",
                0 to "Tắt hẹn giờ"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(0.8.dp, SurfaceDivider, RoundedCornerShape(18.dp))
                    .background(SurfaceCard)
            ) {
                timerOptions.forEachIndexed { index, (mins, label) ->
                    val isSelected = when (mins) {
                        0 -> sleepTimerMinutes == null
                        -1 -> sleepTimerMinutes == -1
                        else -> sleepTimerMinutes == mins
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.97f, enableHaptic = true) {
                                when (mins) {
                                    0 -> onCancelSleepTimer()
                                    -1 -> onSetSleepTimerEndOfTrack()
                                    else -> onSetSleepTimer(mins)
                                }
                                onDismiss()
                            }
                            .padding(horizontal = 18.dp, vertical = 13.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Brand else TextPrimary
                            )
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Brand,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (index < timerOptions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = SurfaceDivider
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(SurfaceActiveIndicator)
                    .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đóng",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                )
            }
        }
    }
}
