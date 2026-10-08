package com.example.onemusic.ui.screens.detail

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
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.components.ApexDialogContainer
import com.example.onemusic.ui.utils.apexBounceClick

/** Hộp thoại đổi tên playlist. Trạng thái ô nhập do [DetailScreen] giữ. */
@Composable
internal fun RenamePlaylistDialog(
    renameInput: String,
    onRenameInputChange: (String) -> Unit,
    onRename: (newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    ApexDialogContainer(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Đổi tên danh sách phát",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = renameInput,
                onValueChange = onRenameInputChange,
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
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Hủy", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(AppTheme.colors.textPrimary)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (renameInput.isNotBlank()) {
                                // Tên mới hiện ngay trên màn → không cần thông báo
                                onRename(renameInput.trim())
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Lưu", color = AppTheme.colors.onInverse, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
