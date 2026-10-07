package com.example.onemusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Stable
class SystemVolumeState(
    private val context: Context,
    private val audioManager: AudioManager?
) {
    var volumeFraction by mutableFloatStateOf(getCurrentFraction())
        private set

    var isInteracting by mutableStateOf(false)

    val isMuted: Boolean
        get() = volumeFraction <= 0.001f

    fun getCurrentFraction(): Float {
        val am = audioManager ?: return 0.5f
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            am.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val range = max - min
        return if (range > 0) {
            ((current - min).toFloat() / range.toFloat()).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }

    fun syncFromSystem() {
        if (!isInteracting) {
            volumeFraction = getCurrentFraction()
        }
    }

    fun onSliderValueChange(fraction: Float) {
        isInteracting = true
        val clamped = fraction.coerceIn(0f, 1f)
        volumeFraction = clamped
        val am = audioManager ?: return
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            am.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        } else {
            0
        }
        val range = max - min
        val targetStep = ((clamped * range) + min).roundToInt().coerceIn(min, max)

        try {
            // Flag 0: tránh bật popup volume mặc định của hệ thống đè lên giao diện
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetStep, 0)
        } catch (_: Exception) {}
    }

    fun onSliderInteractionEnd() {
        isInteracting = false
        // Đồng bộ lại chuẩn xác với nấc số nguyên của hệ thống sau khi thả tay
        volumeFraction = getCurrentFraction()
    }
}

@Composable
fun rememberSystemVolumeState(): SystemVolumeState {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val volumeState = remember { SystemVolumeState(context.applicationContext, audioManager) }

    DisposableEffect(context, audioManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
                        volumeState.syncFromSystem()
                    }
                }
            }
        }

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                volumeState.syncFromSystem()
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
            try {
                context.registerReceiver(receiver, filter)
            } catch (_: Exception) {}
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                contentObserver
            )
        } catch (_: Exception) {}

        // Đảm bảo sync giá trị mới nhất khi khởi tạo
        volumeState.syncFromSystem()

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Exception) {}
        }
    }

    return volumeState
}
