package com.example.onemusic.ui.screens.home

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
import com.example.onemusic.theme.Brand
import com.example.onemusic.theme.CharcoalBlack
import com.example.onemusic.theme.PillShape
import com.example.onemusic.theme.PrimaryIvory
import com.example.onemusic.theme.SurfaceBorderStrong
import com.example.onemusic.theme.SurfaceControl
import com.example.onemusic.theme.SurfaceDivider
import com.example.onemusic.theme.TextPrimary
import com.example.onemusic.theme.TextSecondary

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
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Tên danh sách phát...", color = TextSecondary) },
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Hủy", color = PrimaryIvory, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(PrimaryIvory)
                        .apexBounceClick(scaleDown = 0.92f) {
                            if (name.isNotBlank()) {
                                onCreatePlaylist(name.trim())
                                onNameChange("")
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Tạo", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
