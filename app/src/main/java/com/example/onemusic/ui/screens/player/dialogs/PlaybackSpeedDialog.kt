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
import dev.chrisbanes.haze.HazeState

/**
 * Dialog chọn tốc độ phát (0.5x – 2x) của Now Playing.
 * Chọn một mức → gọi [onSelectSpeed] rồi [onDismiss].
 */
@Composable
internal fun PlaybackSpeedDialog(
    currentSpeed: Float,
    hazeState: HazeState,
    onSelectSpeed: (Float) -> Unit,
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
                    val isSelected = currentSpeed == sp

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .apexBounceClick(scaleDown = 0.97f, enableHaptic = true) {
                                onSelectSpeed(sp)
                                onDismiss()
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
