package com.example.onemusic.ui.screens.folder

import com.example.onemusic.theme.AppTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.ui.components.ApexCircularGlassButton

/** Item 1: nút quay lại, tiêu đề "Thư mục nhạc" và mô tả. */
@Composable
internal fun FolderManagerTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApexCircularGlassButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
                contentDescription = "Quay lại",
                onClick = onBack,
                size = 44.dp,
                iconSize = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Thư mục nhạc",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.colors.textPrimary,
                fontSize = 32.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Chọn các thư mục chứa nhạc trên thiết bị để ứng dụng quét và tự động phát nhạc Hi-Res chất lượng cao.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        )
    }
}

/** Item 2: banner "Đang quét tệp âm thanh..." – hiện/ẩn mờ dần theo [isScanning]. */
@Composable
internal fun FolderScanningBanner(isScanning: Boolean) {
    AnimatedVisibility(
        visible = isScanning,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = AppTheme.colors.surface2
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = AppTheme.colors.accent,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Đang quét tệp âm thanh...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                    )
                    Text(
                        text = "Trích xuất thông số Hi-Res & Album artwork",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTheme.colors.accent
                        )
                    )
                }
            }
        }
    }
}
