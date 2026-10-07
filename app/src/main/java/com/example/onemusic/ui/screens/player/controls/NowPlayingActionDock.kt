package com.example.onemusic.ui.screens.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.onemusic.theme.IvoryMedium
import com.example.onemusic.theme.IvoryMuted
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.ui.screens.player.NowPlayingCenterView
import com.example.onemusic.ui.utils.apexBounceClick
import dev.chrisbanes.haze.HazeState

/**
 * Bottom 3 Utility Actions (1:1 Apple Music - Standalone floating icons without dark pill capsule):
 * - Left: Lyrics (speech bubble / lyrics symbol)
 * - Center: Audio Output / AirPlay / Equalizer
 * - Right: Playing Queue (3-bar queue icon)
 * - Pure AMOLED Minimalism: Icons float directly over dynamic ambient mesh
 * - Aligns vertically with Prev, Play, Next above
 */
@Composable
fun NowPlayingActionDock(
    centerView: NowPlayingCenterView,
    isEqualizerOpen: Boolean,
    onSelectLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSelectQueue: () -> Unit,
    onToggleBackToArtwork: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth() .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 0: Lyrics
        val isLyricsSelected = centerView == NowPlayingCenterView.LYRICS
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isLyricsSelected) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    if (isLyricsSelected) {
                        onToggleBackToArtwork()
                    } else {
                        onSelectLyrics()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = "Lời bài hát",
                tint = if (isLyricsSelected) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }

        // Tab 1: Audio Output / Equalizer DSP / Cast
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isEqualizerOpen) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    onOpenEqualizer()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "Bộ chỉnh âm & DSP",
                tint = if (isEqualizerOpen) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }

        // Tab 2: Queue
        val isQueueSelected = centerView == NowPlayingCenterView.QUEUE
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isQueueSelected) IvoryMuted else Color.Transparent)
                .apexBounceClick(scaleDown = 0.88f, enableHaptic = true) {
                    if (isQueueSelected) {
                        onToggleBackToArtwork()
                    } else {
                        onSelectQueue()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Hàng đợi phát",
                tint = if (isQueueSelected) TextPrimary else IvoryMedium,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
