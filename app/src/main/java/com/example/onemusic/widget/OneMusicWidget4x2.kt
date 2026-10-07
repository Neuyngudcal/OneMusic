package com.example.onemusic.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.MusicPlayerController

/**
 * 4x2 Feature-Packed One UI 8.5 Homescreen Music Player Widget with Shuffle/Repeat.
 */
class OneMusicWidget4x2 : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val controller = MusicPlayerController.getInstance(context)
        OneMusicWidgetUpdater.updateAllWidgets(context, controller.playbackState.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val controller = MusicPlayerController.getInstance(context)

        when (intent.action) {
            OneMusicWidgetUpdater.ACTION_PLAY_PAUSE -> {
                controller.togglePlayPause()
            }
            OneMusicWidgetUpdater.ACTION_NEXT -> {
                controller.skipToNext()
            }
            OneMusicWidgetUpdater.ACTION_PREV -> {
                controller.skipToPrevious()
            }
            OneMusicWidgetUpdater.ACTION_FAVORITE -> {
                val currentTrack = controller.playbackState.value.currentTrack
                if (currentTrack != null) {
                    val repo = MusicRepository.getInstance(context)
                    val newFav = repo.toggleFavorite(currentTrack.id)
                    controller.updateTrackFavorite(currentTrack.id, newFav)
                }
            }
            OneMusicWidgetUpdater.ACTION_SHUFFLE -> {
                controller.toggleShuffle()
            }
            OneMusicWidgetUpdater.ACTION_REPEAT -> {
                controller.cycleRepeatMode()
            }
        }
    }
}
