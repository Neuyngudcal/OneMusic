package com.example.onemusic

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import java.io.File

/**
 * OneMusicApplication: Global Application Entry Point.
 * - Global UncaughtExceptionHandler: Catches critical unhandled exceptions, prevents crash loops.
 * - Coil ImageLoaderFactory: Configures bounded hardware-accelerated memory cache (25%) & disk cache (100MB).
 */
class OneMusicApplication : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "OneMusicApp"
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // 1. Global Safety Net: Setup UncaughtExceptionHandler
        setupGlobalExceptionHandler()

        // 2. Initialize Room Database (Motion Artwork Cache)
        com.example.onemusic.data.local.OneMusicDatabase.getInstance(applicationContext)
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "FATAL CRASH INTERCEPTED on thread " + thread.name + ": " + throwable.message, throwable)
            } catch (_: Throwable) {
                // Ensure no secondary crash occurs during error reporting
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    /**
     * Bounded, Hardware-Accelerated Coil ImageLoader
     * - 25% max available JVM heap memory cache.
     * - 100 MB disk cache for artwork.
     * - Bitmap.Config.HARDWARE support to bypass JVM Heap for graphic rendering.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_artwork_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100 MB
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
