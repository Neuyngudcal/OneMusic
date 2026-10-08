package com.example.onemusic.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Nghe sự kiện của ExoPlayer chính và dịch thành cập nhật `playbackState`:
 * đang phát/dừng, chuyển bài (kể cả gapless), sẵn sàng/hết bài, lỗi phát.
 * Sở hữu bộ đếm lỗi liên tiếp (`consecutivePlaybackErrors`). Việc cần làm ở controller
 * (theo dõi vị trí, nạp lời, ReplayGain, chuyển bài khi lỗi…) đi qua [Callbacks].
 */
internal class PlayerEventListener(
    private val player: () -> ExoPlayer?,
    private val playbackState: MutableStateFlow<PlaybackState>,
    private val callbacks: Callbacks,
) : Player.Listener {

    interface Callbacks {
        /** true khi controller đang sắp lại hàng đợi (moveMediaItem/removeMediaItem) – không phải chuyển bài thật. */
        val isReorderingQueue: Boolean

        /** Bắt đầu / ngừng phát thật sự (đã cập nhật isPlaying). */
        fun onActivePlaybackChanged(isActivelyPlaying: Boolean)

        /** ExoPlayer đã chuyển sang bài [track] ở vị trí [index] của hàng đợi. */
        fun onCurrentTrackChanged(track: Track, index: Int, queue: List<Track>, reason: Int)

        /** Player sẵn sàng phát (STATE_READY) với audio session [audioSessionId]. */
        fun onPlayerReady(audioSessionId: Int)

        /** Bài hiện tại phát hết (STATE_ENDED). */
        fun onTrackEnded()

        /** Lỗi phát nhưng hàng đợi còn bài khác → báo người dùng và chuyển bài. */
        fun onSkipAfterError()
    }

    private var consecutivePlaybackErrors: Int = 0

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val exoPlayer = player()
        val isActivelyPlaying = isPlaying || (exoPlayer?.playWhenReady == true && exoPlayer.playbackState != Player.STATE_ENDED && exoPlayer.playbackState != Player.STATE_IDLE)
        playbackState.value = playbackState.value.copy(isPlaying = isActivelyPlaying)
        callbacks.onActivePlaybackChanged(isActivelyPlaying)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (callbacks.isReorderingQueue) {
            // Sắp xếp thứ tự hàng đợi, không phải chuyển bài hát mới. Bỏ qua để tránh reset playback và nháy UI
            return
        }
        consecutivePlaybackErrors = 0
        // Gapless playback transition handling: seamlessly switch track metadata without re-buffering
        val mediaId = mediaItem?.mediaId ?: return
        val queue = playbackState.value.queue
        val playerIndex = player()?.currentMediaItemIndex ?: -1
        val newIndex = if (playerIndex in queue.indices && queue[playerIndex].id == mediaId) {
            playerIndex
        } else {
            queue.indexOfFirst { it.id == mediaId }
        }
        if (newIndex != -1) {
            callbacks.onCurrentTrackChanged(queue[newIndex], newIndex, queue, reason)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            consecutivePlaybackErrors = 0
            val duration = player()?.duration ?: 0L
            this.playbackState.value = this.playbackState.value.copy(
                durationMs = if (duration > 0) duration else this.playbackState.value.currentTrack?.durationMs ?: 0L
            )
            player()?.audioSessionId?.let { sessionId ->
                callbacks.onPlayerReady(sessionId)
            }
        } else if (playbackState == Player.STATE_ENDED) {
            callbacks.onTrackEnded()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        android.util.Log.e("MusicPlayerController", "Playback error intercepted: ${error.errorCodeName} - ${error.message}")
        consecutivePlaybackErrors++
        val queueSize = playbackState.value.queue.size
        if (consecutivePlaybackErrors < queueSize && queueSize > 1) {
            callbacks.onSkipAfterError()
        } else {
            consecutivePlaybackErrors = 0
            playbackState.value = playbackState.value.copy(isPlaying = false)
            try {
                player()?.stop()
            } catch (_: Throwable) {}
        }
    }
}
