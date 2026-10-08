package com.example.onemusic.ui.screens.folder

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
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

/** Item 5: nút "Thêm thư mục mới" và (khi đã có thư mục) "Quét lại toàn bộ thư mục". */
@Composable
internal fun FolderActionButtons(
    hasFolders: Boolean,
    onAddFolderClick: () -> Unit,
    onRescanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Button 1: Thêm thư mục mới (Primary Ivory Pill with Spring Bounce)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, PillShape, ambientColor = AppTheme.colors.shadow)
                .clip(PillShape)
                .background(AppTheme.colors.textPrimary)
                .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                    onAddFolderClick()
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = AppTheme.colors.onInverse,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thêm thư mục mới",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = AppTheme.colors.onInverse,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        // Button 2: Quét lại toàn bộ (Secondary Ivory Bordered Pill with Spring Bounce)
        if (hasFolders) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, PillShape, ambientColor = AppTheme.colors.shadow)
                    .clip(PillShape)
                    .background(AppTheme.colors.surfaceControl)
                    .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                    .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                        onRescanClick()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quét lại toàn bộ thư mục",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}
