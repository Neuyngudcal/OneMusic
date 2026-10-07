package com.example.onemusic.data.repository

import android.content.Context
import android.net.Uri
import com.example.onemusic.data.dedup.DuplicateAudioDetector
import com.example.onemusic.data.dedup.DuplicateGroup
import com.example.onemusic.data.local.CustomPlaylist
import com.example.onemusic.data.local.FolderInfo
import com.example.onemusic.data.local.FolderPreferences
import com.example.onemusic.data.local.PlaylistPreferences
import com.example.onemusic.data.model.Track
import com.example.onemusic.data.scanner.AppleMusicMotionFetcher
import com.example.onemusic.data.scanner.LocalMusicScanner
import com.example.onemusic.data.model.MotionArtworkEntity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context? = null) {

    companion object {
        @Volatile
        private var instance: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository {
            return instance ?: synchronized(this) {
                instance ?: MusicRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val folderPreferences: FolderPreferences? = context?.let { FolderPreferences(it) }
    private val playlistPreferences: PlaylistPreferences? = context?.let { PlaylistPreferences(it) }
    private val motionFetcher: AppleMusicMotionFetcher? = context?.let { AppleMusicMotionFetcher(it) }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: Flow<List<Track>> = _tracks.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderInfo>>(emptyList())
    val folders: StateFlow<List<FolderInfo>> = _folders.asStateFlow()

    private val _playlists = MutableStateFlow<List<CustomPlaylist>>(emptyList())
    val playlists: StateFlow<List<CustomPlaylist>> = _playlists.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded: StateFlow<Boolean> = _isInitialDataLoaded.asStateFlow()

    init {
        scope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        val savedFolders = folderPreferences?.getFolders().orEmpty()
        val favIds = playlistPreferences?.getFavoriteTrackIds().orEmpty()
        val savedPlaylists = playlistPreferences?.getPlaylists().orEmpty()
        val rawCached = folderPreferences?.getCachedTracks().orEmpty()
        val hiddenIds = folderPreferences?.getHiddenTrackIds().orEmpty()

        _folders.value = savedFolders
        _playlists.value = savedPlaylists

        val shouldFilterShort = try {
            context?.let { com.example.onemusic.data.local.SettingsPreferences(it).getSettings().filterShortAudio } ?: true
        } catch (_: Exception) { true }
        val minDuration = if (shouldFilterShort) LocalMusicScanner.MIN_AUDIO_DURATION_MS else 5_000L

        // 1. Instant RAM emission of cached tracks (Zero-Delay startup)
        val fastValidTracks = rawCached.filter { it.id !in hiddenIds && it.durationMs >= minDuration }
        _tracks.value = fastValidTracks.map { it.copy(isFavorite = favIds.contains(it.id)) }
        _isInitialDataLoaded.value = true

        // 2. Perform non-blocking disk accessibility verification in background
        if (context != null && fastValidTracks.isNotEmpty()) {
            val fullyVerifiedTracks = fastValidTracks.filter { track ->
                LocalMusicScanner.isTrackAccessible(context, track)
            }
            if (fullyVerifiedTracks.size != fastValidTracks.size) {
                _tracks.value = fullyVerifiedTracks.map { it.copy(isFavorite = favIds.contains(it.id)) }
            }
            LocalMusicScanner.cleanOrphanedArtworks(context, fullyVerifiedTracks.map { it.id }.toSet())
        }
    }


    fun refreshStorageAvailability(context: Context) {
        scope.launch {
            val savedFolders = folderPreferences?.getFolders().orEmpty()
            val favIds = playlistPreferences?.getFavoriteTrackIds().orEmpty()

            if (savedFolders.isEmpty()) {
                _tracks.value = emptyList()
                LocalMusicScanner.cleanOrphanedArtworks(context, emptySet())
                return@launch
            }

            val updatedFolders = savedFolders.map { f ->
                val uri = Uri.parse(f.uriString)
                val isOnline = LocalMusicScanner.isFolderAccessible(context, uri)
                if (isOnline) f else f.copy(trackCount = 0)
            }
            _folders.value = updatedFolders

            val accessibleFolders = savedFolders.filter {
                LocalMusicScanner.isFolderAccessible(context, Uri.parse(it.uriString))
            }

            val autoScan = com.example.onemusic.data.local.SettingsPreferences(context).getSettings().autoScanOnLaunch

            if (accessibleFolders.isEmpty()) {
                _tracks.value = emptyList()
                LocalMusicScanner.cleanOrphanedArtworks(context, emptySet())
            } else if (autoScan) {
                val cachedMap = folderPreferences?.getCachedTracks().orEmpty().associateBy { it.audioUrl }
                val allScanned = mutableListOf<Track>()
                for (f in accessibleFolders) {
                    val uri = Uri.parse(f.uriString)
                    val scanned = LocalMusicScanner.scanFolder(context, uri, cachedMap)
                    allScanned.addAll(scanned)
                }
                val distinct = allScanned.distinctBy { it.id }.map {
                    it.copy(isFavorite = favIds.contains(it.id))
                }
                folderPreferences?.saveCachedTracks(distinct)
                _tracks.value = distinct
                LocalMusicScanner.cleanOrphanedArtworks(context, distinct.map { it.id }.toSet())
            } else {
                // If autoScanOnLaunch is disabled, retain cached tracks without disk re-scanning
                val hiddenIds = folderPreferences?.getHiddenTrackIds().orEmpty()
                val cached = folderPreferences?.getCachedTracks().orEmpty()
                val shouldFilterShort = try {
                    com.example.onemusic.data.local.SettingsPreferences(context).getSettings().filterShortAudio
                } catch (_: Exception) { true }
                val minDuration = if (shouldFilterShort) LocalMusicScanner.MIN_AUDIO_DURATION_MS else 5_000L

                val validTracks = cached.filter { track ->
                    track.id !in hiddenIds &&
                    track.durationMs >= minDuration &&
                    LocalMusicScanner.isTrackAccessible(context, track)
                }.map { it.copy(isFavorite = favIds.contains(it.id)) }
                _tracks.value = validTracks
            }
        }
    }

    fun addAndScanFolder(context: Context, folderUri: Uri, displayName: String) {
        val uriStr = folderUri.toString()
        folderPreferences?.addFolder(uriStr, displayName)
        _folders.value = folderPreferences?.getFolders().orEmpty()

        scope.launch {
            _isScanning.value = true
            try {
                val cachedMap = folderPreferences?.getCachedTracks().orEmpty().associateBy { it.audioUrl }
                val scanned = LocalMusicScanner.scanFolder(context, folderUri, cachedMap)
                folderPreferences?.updateFolderTrackCount(uriStr, scanned.size)
                _folders.value = folderPreferences?.getFolders().orEmpty()

                val savedFolders = folderPreferences?.getFolders().orEmpty().filter {
                    LocalMusicScanner.isFolderAccessible(context, Uri.parse(it.uriString))
                }
                val allTracks = mutableListOf<Track>()
                for (f in savedFolders) {
                    if (f.uriString == uriStr) {
                        allTracks.addAll(scanned)
                    } else {
                        val tracks = LocalMusicScanner.scanFolder(context, Uri.parse(f.uriString), cachedMap)
                        allTracks.addAll(tracks)
                    }
                }
                val favIds = playlistPreferences?.getFavoriteTrackIds().orEmpty()
                val distinct = allTracks.distinctBy { it.id }.map {
                    it.copy(isFavorite = favIds.contains(it.id))
                }

                folderPreferences?.saveCachedTracks(distinct)
                _tracks.value = distinct
                LocalMusicScanner.cleanOrphanedArtworks(context, distinct.map { it.id }.toSet())
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun removeFolder(context: Context?, uriString: String) {
        folderPreferences?.removeFolder(uriString)
        _folders.value = folderPreferences?.getFolders().orEmpty()

        val allCached = folderPreferences?.getCachedTracks().orEmpty().filter { !it.audioUrl.startsWith(uriString) }
        folderPreferences?.saveCachedTracks(allCached)
        _tracks.value = allCached
        if (context != null) {
            LocalMusicScanner.cleanOrphanedArtworks(context, allCached.map { it.id }.toSet())
        }
    }

    fun rescanAllMusic(context: Context, onComplete: ((Int) -> Unit)? = null) {
        val savedFolders = folderPreferences?.getFolders().orEmpty()
        if (savedFolders.isEmpty()) {
            _tracks.value = emptyList()
            LocalMusicScanner.cleanOrphanedArtworks(context, emptySet())
            onComplete?.invoke(0)
            return
        }

        scope.launch {
            _isScanning.value = true
            try {
                val cachedMap = folderPreferences?.getCachedTracks().orEmpty().associateBy { it.audioUrl }
                val allScanned = mutableListOf<Track>()
                for (f in savedFolders) {
                    val uri = Uri.parse(f.uriString)
                    if (LocalMusicScanner.isFolderAccessible(context, uri)) {
                        val scanned = LocalMusicScanner.scanFolder(context, uri, cachedMap)
                        folderPreferences?.updateFolderTrackCount(f.uriString, scanned.size)
                        allScanned.addAll(scanned)
                    } else {
                        folderPreferences?.updateFolderTrackCount(f.uriString, 0)
                    }
                }
                _folders.value = folderPreferences?.getFolders().orEmpty()

                val favIds = playlistPreferences?.getFavoriteTrackIds().orEmpty()
                val distinct = allScanned.distinctBy { it.id }.map {
                    it.copy(isFavorite = favIds.contains(it.id))
                }
                folderPreferences?.saveCachedTracks(distinct)
                _tracks.value = distinct
                LocalMusicScanner.cleanOrphanedArtworks(context, distinct.map { it.id }.toSet())
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(distinct.size)
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    // --- FAVORITES MANAGEMENT (PERSISTED) ---

    fun toggleFavorite(trackId: String): Boolean {
        val isFav = playlistPreferences?.toggleFavorite(trackId) ?: false
        val updated = _tracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = isFav) else it
        }
        _tracks.value = updated
        folderPreferences?.saveCachedTracks(updated)
        return isFav
    }

    // --- CUSTOM PLAYLISTS MANAGEMENT (PERSISTED) ---

    fun createPlaylist(name: String): CustomPlaylist? {
        val pl = playlistPreferences?.createPlaylist(name)
        if (pl != null) {
            _playlists.value = playlistPreferences.getPlaylists()
        }
        return pl
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        playlistPreferences?.renamePlaylist(playlistId, newName)
        _playlists.value = playlistPreferences?.getPlaylists().orEmpty()
    }

    fun deletePlaylist(playlistId: String) {
        playlistPreferences?.deletePlaylist(playlistId)
        _playlists.value = playlistPreferences?.getPlaylists().orEmpty()
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String): Boolean {
        val result = playlistPreferences?.addTrackToPlaylist(playlistId, trackId) ?: false
        if (result) {
            _playlists.value = playlistPreferences.getPlaylists()
        }
        return result
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        val result = playlistPreferences?.removeTrackFromPlaylist(playlistId, trackId) ?: false
        if (result) {
            _playlists.value = playlistPreferences.getPlaylists()
        }
        return result
    }

    fun getTracksForPlaylist(playlist: CustomPlaylist): List<Track> {
        val trackMap = _tracks.value.associateBy { it.id }
        return playlist.trackIds.mapNotNull { trackMap[it] }
    }

    // --- SMART DUPLICATE MANAGEMENT ---

    suspend fun getDuplicateGroups(context: Context): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        DuplicateAudioDetector.findDuplicates(context, _tracks.value)
    }

    suspend fun hideDuplicateTracks(context: Context, trackIdsToHide: Set<String>) = withContext(Dispatchers.IO) {
        folderPreferences?.hideTracks(trackIdsToHide)
        val updated = _tracks.value.filter { it.id !in trackIdsToHide }
        _tracks.value = updated
        folderPreferences?.saveCachedTracks(updated)
        LocalMusicScanner.cleanOrphanedArtworks(context, updated.map { it.id }.toSet())
    }

    suspend fun deleteDuplicateFiles(context: Context, trackIdsToDelete: Set<String>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        val tracksToDelete = _tracks.value.filter { it.id in trackIdsToDelete }

        for (track in tracksToDelete) {
            try {
                val uri = Uri.parse(track.audioUrl)
                if (uri.scheme == "file") {
                    val path = uri.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.delete()) {
                            deletedCount++
                        }
                    }
                } else if (uri.scheme == "content") {
                    val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, uri)
                    if (doc != null && doc.delete()) {
                        deletedCount++
                    } else {
                        val deletedRows = context.contentResolver.delete(uri, null, null)
                        if (deletedRows > 0) deletedCount++
                    }
                }
            } catch (_: Exception) {}
        }

        val updated = _tracks.value.filter { it.id !in trackIdsToDelete }
        _tracks.value = updated
        folderPreferences?.saveCachedTracks(updated)
        LocalMusicScanner.cleanOrphanedArtworks(context, updated.map { it.id }.toSet())
        deletedCount
    }

    // --- MOTION ARTWORK MANAGEMENT ---

    suspend fun getMotionArtwork(track: Track): MotionArtworkEntity? {
        return motionFetcher?.fetchMotionArtworkIfNeeded(track)
    }

    suspend fun getLocalMotionVideoPath(track: Track): String? {
        return motionFetcher?.getLocalMotionVideoPath(track)
    }

    suspend fun getAllDownloadedMotionArtworks(): List<MotionArtworkEntity> {
        return motionFetcher?.getAllDownloadedEntities() ?: emptyList()
    }

    fun startBackgroundMotionScan(tracks: List<Track>) {
        motionFetcher?.startBackgroundMotionScan(tracks)
    }

    fun cancelBackgroundMotionScan() {
        motionFetcher?.cancelBackgroundScan()
    }

    suspend fun clearAllMotionArtworkCache() {
        motionFetcher?.clearAllMotionArtworkCache()
    }

    suspend fun removeMotionArtworkForTrack(track: Track) {
        motionFetcher?.removeMotionArtworkForTrack(track)
    }
}
