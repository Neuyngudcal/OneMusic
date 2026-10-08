package com.example.onemusic.ui.components

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.theme.PillShape
import com.example.onemusic.ui.utils.apexBounceClick

// Phần giao diện dùng chung (giống hệt nhau) của AddToPlaylistDialog và AddToPlaylistMultipleDialog.

/** Tiêu đề "Thêm vào danh sách phát". */
@Composable
internal fun AddToPlaylistTitle() {
    Text(
        text = "Thêm vào danh sách phát",
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            fontSize = 20.sp
        )
    )
}

/** Ô nhập tên khi tạo danh sách phát mới. */
@Composable
internal fun NewPlaylistNameField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
}

/** Nút "Đóng" ở cuối hộp thoại. */
@Composable
internal fun AddToPlaylistCloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(AppTheme.colors.surfaceControl)
            .border(1.5.dp, AppTheme.colors.borderStrong, PillShape)
            .apexBounceClick(scaleDown = 0.96f, enableHaptic = true) {
                onDismiss()
            }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Đóng",
            color = AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
