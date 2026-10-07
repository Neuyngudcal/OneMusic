package com.example.onemusic.playback.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.onemusic.R
import com.example.onemusic.playback.MusicPlayerController

/**
 * Foreground MediaSessionService for robust background audio playback,
 * system media notifications, lock screen controls, and Android auto / bluetooth support.
 */
class OneMusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "onemusic_playback_channel"
        const val CHANNEL_NAME = "Phát nhạc OneMusic"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.playback_notification_channel_name)
                .build()
            setMediaNotificationProvider(notificationProvider)
        } catch (_: Exception) {}

        val controller = MusicPlayerController.getInstance(applicationContext)
        val session = controller.getMediaSession()
        if (session != null) {
            addSession(session)
        }

        // Register real-time background MediaStore ContentObserver
        val musicRepository = com.example.onemusic.data.repository.MusicRepository.getInstance(applicationContext)
        contentObserver = com.example.onemusic.data.scanner.MusicContentObserver(applicationContext, musicRepository).apply {
            register()
        }
    }

    private var contentObserver: com.example.onemusic.data.scanner.MusicContentObserver? = null

    override fun onDestroy() {
        contentObserver?.unregister()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val controller = MusicPlayerController.getInstance(applicationContext)
        return controller.getMediaSession()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val controller = MusicPlayerController.getInstance(applicationContext)
        val player = controller.getExoPlayer()
        // If nothing is playing or player is null, stop the service gracefully
        if (player == null || !player.isPlaying) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trình điều khiển phát nhạc OneMusic trên thanh thông báo và màn hình khóa"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
