package com.example.onemusic.data.scanner

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.onemusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background ContentObserver that listens for real-time audio file insertions,
 * deletions, and modifications in Android MediaStore and automatically refreshes the music library.
 * Includes debouncing (1500ms) to prevent redundant scanning when multiple files are downloaded at once.
 */
class MusicContentObserver(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var debounceJob: Job? = null
    private var isRegistered = false

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        triggerDebouncedSync()
    }

    private fun triggerDebouncedSync() {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(1500) // 1.5s debounce
            try {
                musicRepository.rescanAllMusic(context)
            } catch (_: Exception) {}
        }
    }

    fun register() {
        if (isRegistered) return
        try {
            val contentResolver = context.contentResolver
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                this
            )
            isRegistered = true
        } catch (_: Exception) {}
    }

    fun unregister() {
        if (!isRegistered) return
        try {
            context.contentResolver.unregisterContentObserver(this)
            isRegistered = false
        } catch (_: Exception) {}
    }
}
