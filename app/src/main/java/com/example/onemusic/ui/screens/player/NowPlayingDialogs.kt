package com.example.onemusic.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import com.example.onemusic.playback.PlaybackState
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.IvoryStroke
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceActiveIndicator
import com.example.onemusic.theme.SurfaceCard
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.SurfaceElevated
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.apexFrostedGlass
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Hộp thoại chọn tốc độ phát nhạc (0.5x -> 2.0x)
 */
@Composable
fun PlaybackSpeedDialog(
    playbackSpeed: Float,
    onDismissRequest: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    hazeState: HazeState? = null
) {
    ApexDialogContainer(
        onDismissRequest = onDismissRequest,
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Tốc độ phát",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(0.8.dp, SurfaceDivider, RoundedCornerShape(18.dp))
                    .background(SurfaceCard)
            ) {
                speedOptions.forEachIndexed { index, sp ->
                    val isSelected = playbackSpeed == sp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.97f, enableHaptic = true) {
                                onSpeedSelected(sp)
                                onDismissRequest()
                            }
                            .padding(horizontal = 18.dp, vertical = 13.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${sp}x",
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

                    if (index < speedOptions.lastIndex) {
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
                        onDismissRequest()
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

/**
 * Hộp thoại hẹn giờ tắt nhạc (Sleep Timer)
 */
@Composable
fun SleepTimerDialog(
    playbackState: PlaybackState,
    onDismissRequest: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSetSleepTimerEndOfTrack: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    hazeState: HazeState? = null
) {
    ApexDialogContainer(
        onDismissRequest = onDismissRequest,
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

            if (playbackState.sleepTimerRemainingSeconds != null) {
                val rem = playbackState.sleepTimerRemainingSeconds
                val mins = rem / 60
                val secs = rem % 60
                val display = if (rem < 0) "Sau khi kết thúc bài hát" else String.format("%02d:%02d", mins, secs)

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
                        0 -> playbackState.sleepTimerMinutes == null
                        -1 -> playbackState.sleepTimerMinutes == -1
                        else -> playbackState.sleepTimerMinutes == mins
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
                            onDismissRequest()
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
                        onDismissRequest()
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

/**
 * Toast thông báo thêm/bỏ bài hát yêu thích dạng viên thuốc nổi (1:1 Apple Music)
 */
@Composable
fun NowPlayingFavoriteToast(
    message: String?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier
            .padding(bottom = 36.dp)
            .navigationBarsPadding(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .border(0.7.dp, IvoryStroke, RoundedCornerShape(18.dp))
                .apexFrostedGlass(
                    backgroundColor = SurfaceElevated.copy(alpha = 0.92f),
                    blurRadius = 24.dp,
                    hazeState = hazeState
                )
                .padding(horizontal = 20.dp, vertical = 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isFavoritedMsg = message?.contains("Đã ưa thích") == true
                Icon(
                    imageVector = if (isFavoritedMsg) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = PrimaryIvory,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                )
            }
        }
    }
}
