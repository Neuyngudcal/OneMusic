package com.example.onemusic.ui.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Chiều cao cụm NowBar + thanh tab nổi ở đáy, đã cộng thanh điều hướng hệ thống và 16dp khoảng thở.
 * Được đo thật ở MainActivity rồi "phát" xuống mọi màn, dùng làm khoảng đệm dưới cho danh sách
 * để nội dung cuối không bị cụm nổi che (thay cho các số cứng 150.dp / 100.dp + navBottom).
 */
val LocalBottomOverlayPadding = compositionLocalOf<Dp> { 180.dp }
