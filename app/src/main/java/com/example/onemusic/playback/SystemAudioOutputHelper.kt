package com.example.onemusic.playback

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object SystemAudioOutputHelper {

    /**
     * Mở trực tiếp bảng điều khiển "Đầu ra đa phương tiện / Media Output" chính thức
     * của Samsung One UI (Galaxy S24/S25) và Android 11+ (API 30+).
     */
    fun openMediaOutput(context: Context) {
        var launched = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val panelActions = listOf(
                "android.settings.panel.action.MEDIA_OUTPUT",
                "com.android.settings.panel.action.MEDIA_OUTPUT"
            )

            for (action in panelActions) {
                try {
                    val intent = Intent(action).apply {
                        putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                        putExtra("android.provider.extra.PACKAGE_NAME", context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    launched = true
                    break
                } catch (_: Exception) {
                    // Tiếp tục thử action kế tiếp nếu ROM tùy biến khác nhau
                }
            }
        }

        if (!launched) {
            openBluetoothSettings(context)
        }
    }

    fun openBluetoothSettings(context: Context) {
        try {
            val btIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(btIntent)
        } catch (_: Exception) {
            try {
                val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(soundIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Không thể mở cài đặt âm thanh", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
