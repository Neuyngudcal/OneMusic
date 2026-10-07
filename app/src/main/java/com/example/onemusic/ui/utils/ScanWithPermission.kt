package com.example.onemusic.ui.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Trả về hàm: có quyền đọc nhạc thì quét luôn, chưa có thì xin quyền rồi quét.
 * Gom logic xin quyền + quét về một chỗ (trước đây bị copy ở Home và Library, còn Cài đặt thì không xin quyền).
 */
@Composable
fun rememberScanWithPermission(onRescan: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val showSnackbar = LocalAppSnackbar.current
    val currentOnRescan by rememberUpdatedState(onRescan)
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            currentOnRescan()
        } else {
            // Nói rõ bước tiếp theo: cấp quyền trong Cài đặt hệ thống
            showSnackbar("Cần quyền truy cập nhạc để quét bài hát", "Mở cài đặt") {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
        }
    }
    return remember(launcher) {
        {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                currentOnRescan()
            } else {
                launcher.launch(permission)
            }
        }
    }
}
