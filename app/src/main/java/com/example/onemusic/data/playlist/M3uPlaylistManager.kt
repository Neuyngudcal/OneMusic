package com.example.onemusic.data.playlist

import android.content.Context
import android.net.Uri
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class M3uTrackEntry(
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val pathOrUri: String
)

object M3uPlaylistManager {

    /**
     * Generates a standard UTF-8 #EXTM3U (.m3u8) string representation of a playlist.
     */
    fun exportPlaylistToM3uString(playlist: CustomPlaylist, allTracks: List<Track>): String {
        val tracksMap = allTracks.associateBy { it.id }
        val playlistTracks = playlist.trackIds.mapNotNull { tracksMap[it] }

        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:${playlist.name}")
        sb.appendLine()

        for (track in playlistTracks) {
            val durationSec = (track.durationMs / 1000).coerceAtLeast(0)
            sb.appendLine("#EXTINF:$durationSec,${track.artist} - ${track.title}")
            sb.appendLine(track.audioUrl)
        }

        return sb.toString()
    }

    /**
     * Writes the M3U content string directly into a SAF document URI.
     */
    suspend fun writeContentToUri(context: Context, uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parses an M3U / M3U8 file from a SAF Uri and matches entries against available library tracks.
     * Returns Pair(PlaylistName, ListOfMatchedTrackIds).
     */
    suspend fun importPlaylistFromUri(
        context: Context,
        uri: Uri,
        allTracks: List<Track>
    ): Pair<String, List<String>>? = withContext(Dispatchers.IO) {
        return@withContext try {
            var playlistName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Playlist Nhập"
            if (playlistName.isBlank() || playlistName.startsWith("primary:")) {
                playlistName = "Playlist Mới"
            }

            val entries = mutableListOf<M3uTrackEntry>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var currentTitle = ""
                    var currentArtist = ""
                    var currentDuration = 0

                    reader.forEachLine { rawLine ->
                        val line = rawLine.trim()
                        when {
                            line.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                                val name = line.substringAfter(':').trim()
                                if (name.isNotBlank()) {
                                    playlistName = name
                                }
                            }
                            line.startsWith("#EXTINF:", ignoreCase = true) -> {
                                val info = line.substringAfter(':').trim()
                                val durationStr = info.substringBefore(',').trim()
                                currentDuration = durationStr.toIntOrNull() ?: 0
                                val titleArtist = info.substringAfter(',').trim()
                                if (titleArtist.contains(" - ")) {
                                    currentArtist = titleArtist.substringBefore(" - ").trim()
                                    currentTitle = titleArtist.substringAfter(" - ").trim()
                                } else {
                                    currentArtist = ""
                                    currentTitle = titleArtist
                                }
                            }
                            line.isNotBlank() && !line.startsWith("#") -> {
                                entries.add(
                                    M3uTrackEntry(
                                        title = currentTitle,
                                        artist = currentArtist,
                                        durationSeconds = currentDuration,
                                        pathOrUri = line
                                    )
                                )
                                currentTitle = ""
                                currentArtist = ""
                                currentDuration = 0
                            }
                        }
                    }
                }
            }

            // Match entries with library tracks
            val matchedTrackIds = mutableListOf<String>()
            for (entry in entries) {
                val matched = allTracks.firstOrNull { t ->
                    t.audioUrl.equals(entry.pathOrUri, ignoreCase = true) ||
                            (entry.pathOrUri.isNotBlank() && t.audioUrl.endsWith(entry.pathOrUri.substringAfterLast('/'), ignoreCase = true)) ||
                            (entry.title.isNotBlank() && t.title.equals(entry.title, ignoreCase = true) &&
                                    (entry.artist.isBlank() || t.artist.contains(entry.artist, ignoreCase = true)))
                }
                if (matched != null && matched.id !in matchedTrackIds) {
                    matchedTrackIds.add(matched.id)
                }
            }

            Pair(playlistName, matchedTrackIds)
        } catch (_: Exception) {
            null
        }
    }
}
