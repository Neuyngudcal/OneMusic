package com.example.onemusic.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Immutable
@Serializable
data class CustomPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trackIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)


class PlaylistPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("onemusic_playlist_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- FAVORITES PERSISTENCE ---

    fun getFavoriteTrackIds(): Set<String> {
        return prefs.getStringSet("favorite_track_ids", emptySet()) ?: emptySet()
    }

    fun saveFavoriteTrackIds(ids: Set<String>) {
        prefs.edit().putStringSet("favorite_track_ids", ids).apply()
    }

    fun toggleFavorite(trackId: String): Boolean {
        val current = getFavoriteTrackIds().toMutableSet()
        val isNowFav = if (current.contains(trackId)) {
            current.remove(trackId)
            false
        } else {
            current.add(trackId)
            true
        }
        saveFavoriteTrackIds(current)
        return isNowFav
    }

    // --- CUSTOM PLAYLISTS PERSISTENCE ---

    fun getPlaylists(): List<CustomPlaylist> {
        val raw = prefs.getString("custom_playlists", null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<CustomPlaylist>) {
        try {
            val raw = json.encodeToString(playlists)
            prefs.edit().putString("custom_playlists", raw).apply()
        } catch (_: Throwable) {}
    }

    fun createPlaylist(name: String): CustomPlaylist {
        val playlist = CustomPlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            trackIds = emptyList()
        )
        val current = getPlaylists().toMutableList()
        current.add(0, playlist)
        savePlaylists(current)
        return playlist
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val current = getPlaylists().map {
            if (it.id == playlistId) it.copy(name = newName.trim()) else it
        }
        savePlaylists(current)
    }

    fun deletePlaylist(playlistId: String) {
        val current = getPlaylists().filter { it.id != playlistId }
        savePlaylists(current)
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String): Boolean {
        var added = false
        val current = getPlaylists().map {
            if (it.id == playlistId) {
                if (!it.trackIds.contains(trackId)) {
                    added = true
                    it.copy(trackIds = it.trackIds + trackId)
                } else it
            } else it
        }
        if (added) {
            savePlaylists(current)
        }
        return added
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        var removed = false
        val current = getPlaylists().map {
            if (it.id == playlistId) {
                if (it.trackIds.contains(trackId)) {
                    removed = true
                    it.copy(trackIds = it.trackIds - trackId)
                } else it
            } else it
        }
        if (removed) {
            savePlaylists(current)
        }
        return removed
    }
}
