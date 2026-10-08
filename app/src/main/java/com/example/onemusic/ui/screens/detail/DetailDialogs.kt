package com.example.onemusic.ui.screens.detail

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
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
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
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = renameInput,
                onValueChange = onRenameInputChange,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand,
                    unfocusedBorderColor = SurfaceDivider,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                        .background(SurfaceControl)
                        .border(1.5.dp, SurfaceBorderStrong, PillShape)
                        .apexBounceClick(scaleDown = 0.92f) { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Hủy", color = PrimaryIvory, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(PrimaryIvory)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (renameInput.isNotBlank()) {
                                // Tên mới hiện ngay trên màn → không cần thông báo
                                onRename(renameInput.trim())
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Lưu", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
