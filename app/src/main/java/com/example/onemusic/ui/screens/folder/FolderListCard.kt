package com.example.onemusic.ui.screens.folder

import com.example.onemusic.theme.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onemusic.data.local.FolderInfo
import com.example.onemusic.theme.ApexAmber
import com.example.onemusic.theme.ApexRose
import com.example.onemusic.theme.apexGlassCard
import com.example.onemusic.ui.components.ApexCircularGlassButton

/** Item 3: thẻ "THƯ MỤC ĐANG QUẢN LÝ (n)" – trạng thái trống hoặc danh sách thư mục kèm nút xóa. */
@Composable
internal fun FolderListCard(
    folders: List<FolderInfo>,
    onRemoveClick: (uriString: String, displayName: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "THƯ MỤC ĐANG QUẢN LÝ (${folders.size})",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textSecondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .apexGlassCard(shape = RoundedCornerShape(26.dp))
        ) {
            if (folders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceActiveIndicator),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = ApexAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Chưa có thư mục nào",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Nhấn nút 'Thêm thư mục mới' bên dưới để quét các bài hát yêu thích của bạn.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    folders.forEachIndexed { index, folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppTheme.colors.surfaceActiveIndicator),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null, // biểu tượng trang trí, tên thư mục đã có ở cạnh
                                    tint = ApexAmber,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${folder.trackCount} bài hát tìm thấy",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AppTheme.colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            ApexCircularGlassButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "Xóa thư mục",
                                // Chỉ mở hộp xác nhận, chưa xóa ngay
                                onClick = { onRemoveClick(folder.uriString, folder.displayName) },
                                size = 38.dp,
                                iconSize = 18.dp,
                                iconTint = ApexRose.copy(alpha = 0.85f),
                                backgroundColor = AppTheme.colors.surfaceActiveIndicator.copy(alpha = 0.60f)
                            )
                        }

                        if (index < folders.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 74.dp, end = 18.dp),
                                thickness = 0.5.dp,
                                color = AppTheme.colors.divider
                            )
                        }
                    }
                }
            }
        }
    }
}
