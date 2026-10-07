package com.example.onemusic.ui.screens.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.ui.components.AppleBackwardIcon
import com.example.onemusic.ui.components.AppleForwardIcon
import com.example.onemusic.ui.components.ApplePauseIcon
import com.example.onemusic.ui.components.ApplePlayIcon
import com.example.onemusic.ui.utils.apexBounceClick

/**
 * 3 nút chính của Now Playing: bài trước, phát/tạm dừng, bài kế.
 * Việc chống bấm liên tục cho bài trước / bài kế do nơi gọi xử lý trong [onPrevious] / [onNext].
 */
@Composable
internal fun MasterPlaybackControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 52.dp)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    onPrevious()
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
                    onNext()
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
