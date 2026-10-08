package com.example.onemusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.playback.AudioEffectManager
import com.example.onemusic.playback.MusicPlayerController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.net.Uri
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.withContext
import com.example.onemusic.theme.OneMusicTheme
import com.example.onemusic.data.local.SettingsPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.onemusic.ui.main.OneMusicApp

class MainActivity : ComponentActivity() {

    private lateinit var musicRepository: MusicRepository
    private lateinit var audioEffectManager: AudioEffectManager
    private lateinit var playerController: MusicPlayerController
    private val externalPlayTrigger = mutableStateOf(0L)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Notification permission result handled
    }

    private val storageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            context?.let {
                musicRepository.refreshStorageAvailability(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android Core SplashScreen with instant dismissal
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Initialize repository and fast controller instances
        musicRepository = MusicRepository.getInstance(applicationContext)
        playerController = MusicPlayerController.getInstance(applicationContext)
        audioEffectManager = playerController.audioEffectManager

        // Handle possible external audio VIEW intent on start
        handleAudioIntent(intent)

        // 3. Direct UI render with true zero artificial delay (instant 120Hz display)
        setContent {
            val themeSettings by SettingsPreferences.getInstance(applicationContext)
                .settingsFlow.collectAsState()
            OneMusicTheme(themeMode = themeSettings.themeMode) {
                LaunchedEffect(Unit) {
                    requestNotificationPermission()
                }
                OneMusicApp(
                    musicRepository = musicRepository,
                    playerController = playerController,
                    externalPlayTrigger = externalPlayTrigger.value
                )
            }
        }

        // 4. Asynchronously register storage receivers with RECEIVER_NOT_EXPORTED
        lifecycleScope.launch(Dispatchers.IO) {
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_MEDIA_UNMOUNTED)
                addAction(android.content.Intent.ACTION_MEDIA_EJECT)
                addAction(android.content.Intent.ACTION_MEDIA_REMOVED)
                addAction(android.content.Intent.ACTION_MEDIA_BAD_REMOVAL)
                addAction(android.content.Intent.ACTION_MEDIA_MOUNTED)
                addDataScheme("file")
            }
            try {
                ContextCompat.registerReceiver(
                    this@MainActivity,
                    storageReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (_: Throwable) {}
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAudioIntent(intent)
    }

    private fun handleAudioIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            lifecycleScope.launch(Dispatchers.IO) {
                val track = extractTrackFromUri(uri)
                if (track != null) {
                    withContext(Dispatchers.Main) {
                        playerController.setQueue(listOf(track), startIndex = 0, autoPlay = true)
                        externalPlayTrigger.value = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun extractTrackFromUri(uri: Uri): Track? {
        return try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(applicationContext, uri)
            } catch (_: Exception) {
                return null
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.ifBlank { null }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "External Audio"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.ifBlank { null } ?: "Unknown Artist"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()

            Track(
                id = "ext_${System.currentTimeMillis()}_${uri.hashCode()}",
                title = title,
                artist = artist,
                album = "Tệp ngoài",
                durationMs = durationMs,
                audioUrl = uri.toString(),
                artworkUrl = "",
                isFavorite = false
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        musicRepository.refreshStorageAvailability(this)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(storageReceiver)
        } catch (_: Exception) {}
    }
}
