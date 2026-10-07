package com.example.onemusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.widget.RemoteViews
import com.example.onemusic.MainActivity
import com.example.onemusic.R
import com.example.onemusic.playback.MusicPlayerController
import com.example.onemusic.playback.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OneMusicWidgetUpdater {

    const val ACTION_PLAY_PAUSE = "com.example.onemusic.WIDGET_PLAY_PAUSE"
    const val ACTION_NEXT = "com.example.onemusic.WIDGET_NEXT"
    const val ACTION_PREV = "com.example.onemusic.WIDGET_PREV"
    const val ACTION_FAVORITE = "com.example.onemusic.WIDGET_FAVORITE"
    const val ACTION_SHUFFLE = "com.example.onemusic.WIDGET_SHUFFLE"
    const val ACTION_REPEAT = "com.example.onemusic.WIDGET_REPEAT"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun updateAllWidgets(context: Context, state: PlaybackState) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        scope.launch {
            val track = state.currentTrack
            val isPlaying = state.isPlaying
            val isFavorite = track?.isFavorite == true

            val coverBitmap = if (track != null && track.artworkUrl.isNotBlank()) {
                loadAndCropSquircleBitmap(context, track.artworkUrl, 160)
            } else {
                null
            }

            withContext(Dispatchers.Main) {
                runCatching {
                    // Update 4x1 Widgets
                    val component4x1 = ComponentName(context, OneMusicWidget4x1::class.java)
                    val ids4x1 = appWidgetManager.getAppWidgetIds(component4x1)
                    if (ids4x1.isNotEmpty()) {
                        val views4x1 = build4x1RemoteViews(context, state, coverBitmap)
                        appWidgetManager.updateAppWidget(ids4x1, views4x1)
                    }

                    // Update 4x2 Widgets
                    val component4x2 = ComponentName(context, OneMusicWidget4x2::class.java)
                    val ids4x2 = appWidgetManager.getAppWidgetIds(component4x2)
                    if (ids4x2.isNotEmpty()) {
                        val views4x2 = build4x2RemoteViews(context, state, coverBitmap)
                        appWidgetManager.updateAppWidget(ids4x2, views4x2)
                    }
                }
            }
        }
    }

    private fun build4x1RemoteViews(
        context: Context,
        state: PlaybackState,
        coverBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_onemusic_4x1)
        val track = state.currentTrack

        // Song Info
        if (track != null) {
            views.setTextViewText(R.id.widget_track_title, track.title)
            views.setTextViewText(R.id.widget_track_artist, track.artist)
        } else {
            views.setTextViewText(R.id.widget_track_title, "OneMusic")
            views.setTextViewText(R.id.widget_track_artist, "Chạm để phát nhạc")
        }

        // Cover Art
        if (coverBitmap != null) {
            views.setImageViewBitmap(R.id.widget_cover_art, coverBitmap)
        } else {
            views.setImageViewResource(R.id.widget_cover_art, R.drawable.app_logo)
        }

        // Play / Pause Icon
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        // Favorite Icon
        views.setImageViewResource(
            R.id.widget_btn_favorite,
            if (track?.isFavorite == true) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart
        )

        // PendingIntents
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, getPendingIntent(context, ACTION_PLAY_PAUSE, 101))
        views.setOnClickPendingIntent(R.id.widget_btn_next, getPendingIntent(context, ACTION_NEXT, 102))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, getPendingIntent(context, ACTION_PREV, 103))
        views.setOnClickPendingIntent(R.id.widget_btn_favorite, getPendingIntent(context, ACTION_FAVORITE, 104))

        // Clicking on Title or Artwork opens Main App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 201, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_cover_art, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)

        return views
    }

    private fun build4x2RemoteViews(
        context: Context,
        state: PlaybackState,
        coverBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_onemusic_4x2)
        val track = state.currentTrack

        // Song Info
        if (track != null) {
            views.setTextViewText(R.id.widget_track_title, track.title)
            views.setTextViewText(R.id.widget_track_artist, track.artist)
            views.setTextViewText(R.id.widget_audio_quality, "One UI 8.5 • ${track.bitRate}")
        } else {
            views.setTextViewText(R.id.widget_track_title, "OneMusic")
            views.setTextViewText(R.id.widget_track_artist, "Samsung One UI 8.5 Experience")
            views.setTextViewText(R.id.widget_audio_quality, "Lossless Audio")
        }

        // Cover Art
        if (coverBitmap != null) {
            views.setImageViewBitmap(R.id.widget_cover_art, coverBitmap)
        } else {
            views.setImageViewResource(R.id.widget_cover_art, R.drawable.app_logo)
        }

        // Play / Pause Icon
        views.setImageViewResource(
            R.id.widget_btn_play_pause,
            if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        // Favorite Icon
        views.setImageViewResource(
            R.id.widget_btn_favorite,
            if (track?.isFavorite == true) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart
        )

        // PendingIntents
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, getPendingIntent(context, ACTION_PLAY_PAUSE, 301))
        views.setOnClickPendingIntent(R.id.widget_btn_next, getPendingIntent(context, ACTION_NEXT, 302))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, getPendingIntent(context, ACTION_PREV, 303))
        views.setOnClickPendingIntent(R.id.widget_btn_favorite, getPendingIntent(context, ACTION_FAVORITE, 304))
        views.setOnClickPendingIntent(R.id.widget_btn_shuffle, getPendingIntent(context, ACTION_SHUFFLE, 305))
        views.setOnClickPendingIntent(R.id.widget_btn_repeat, getPendingIntent(context, ACTION_REPEAT, 306))

        // Clicking on Top Section opens Main App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 401, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_top_section, openAppPendingIntent)

        return views
    }

    private fun getPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadAndCropSquircleBitmap(context: Context, uriString: String, sizePx: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBitmap = BitmapFactory.decodeStream(stream)
            stream.close()

            if (rawBitmap == null) return null

            val scaledBitmap = Bitmap.createScaledBitmap(rawBitmap, sizePx, sizePx, true)
            val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, sizePx, sizePx)
            val rectF = RectF(rect)
            val radius = sizePx * 0.22f // Squircle corner radius

            canvas.drawRoundRect(rectF, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaledBitmap, rect, rect, paint)

            output
        } catch (_: Exception) {
            null
        }
    }
}
