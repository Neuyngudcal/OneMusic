package com.example.onemusic.ui.screens.dedup

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.utils.apexBounceClick

/** Dock nổi phía dưới: "Ẩn (n)" (chỉ ẩn khỏi thư viện) và "Xóa File (n)" (mở hộp thoại xác nhận). */
@Composable
internal fun DuplicateActionDock(
    totalSelectedCount: Int,
    onHideClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .navigationBarsPadding()
            .shadow(16.dp, PillShape, ambientColor = AppTheme.colors.shadow)
            .clip(PillShape)
            .border(0.85.dp, AppTheme.colors.pillBorderBrush, PillShape)
            .background(AppTheme.colors.surface1.copy(alpha = 0.94f))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Chỉ ẩn khỏi thư viện (An toàn)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(AppTheme.colors.surfaceControl)
                    .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                        onHideClick()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ẩn ($totalSelectedCount)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }

            // Button 2: Xóa vĩnh viễn file trên đĩa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(AppTheme.colors.danger.copy(alpha = 0.18f))
                    .apexBounceClick(scaleDown = 0.95f, enableHaptic = true) {
                        onDeleteClick()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = AppTheme.colors.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Xóa File ($totalSelectedCount)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.danger,
                            fontSize = 13.5.sp
                        )
                    )
                }
            }
        }
    }
}
