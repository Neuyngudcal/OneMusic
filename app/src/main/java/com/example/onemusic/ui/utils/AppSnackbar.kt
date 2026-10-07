package com.example.onemusic.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * showSnackbar(message, actionLabel?, onAction?)
 * - actionLabel/onAction khác null → Snackbar có nút (vd "Hoàn tác").
 */
typealias ShowSnackbar = (message: String, actionLabel: String?, onAction: (() -> Unit)?) -> Unit

/** Snackbar dùng chung của app, cung cấp từ MainActivity. Mặc định không làm gì (vd trong Preview). */
val LocalAppSnackbar = staticCompositionLocalOf<ShowSnackbar> { { _, _, _ -> } }
