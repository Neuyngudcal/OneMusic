package com.example.onemusic.ui.screens.home

import com.example.onemusic.theme.AppTheme
import dev.chrisbanes.haze.HazeState
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.utils.apexBounceClick
import com.example.onemusic.theme.PillShape

/** Hộp thoại tạo playlist mới. Tên đang gõ do màn cha giữ để không mất khi đóng/mở lại hộp thoại. */
@Composable
internal fun NewPlaylistDialog(
    name: String,
    onNameChange: (String) -> Unit,
    hazeState: HazeState,
    onCreatePlaylist: (String) -> Unit,
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
                text = "Tạo Danh Sách Phát Mới",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Tên danh sách phát...", color = AppTheme.colors.textSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.accent,
                    unfocusedBorderColor = AppTheme.colors.divider,
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(AppTheme.colors.surfaceControl)
                        .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
                        .apexBounceClick(scaleDown = 0.92f) { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Hủy", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(AppTheme.colors.textPrimary)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (name.isNotBlank()) {
                                onCreatePlaylist(name.trim())
                                onNameChange("")
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Tạo", color = AppTheme.colors.onInverse, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
