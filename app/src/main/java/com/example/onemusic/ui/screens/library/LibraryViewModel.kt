package com.example.onemusic.ui.screens.library

import androidx.lifecycle.ViewModel
import com.example.onemusic.data.local.SettingsPreferences
import com.example.onemusic.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Main management filter tabs in LibraryScreen.
 */
enum class LibraryTab(val title: String) {
    SONGS("Tất cả bài hát"),
    ALBUMS("Album"),
    ARTISTS("Nghệ sĩ"),
    PLAYLISTS("Playlist")
}

/**
 * Filter and sort options for the song list in LibraryScreen.
 */
enum class SongSortOption(val title: String) {
    ALL("Tất cả"),
    TITLE_AZ("Tên A-Z"),
    ARTIST("Nghệ sĩ"),
    DURATION("Thời lượng")
}

/**
 * View mode options for the song list in LibraryScreen.
 */
enum class LibraryViewMode(val title: String) {
    LIST("Danh sách"),
    GRID_2("Lưới 2 cột"),
    GRID_3("Lưới 3 cột")
}

data class LibraryUiState(
    val currentTab: LibraryTab = LibraryTab.SONGS,
    val sortOption: SongSortOption = SongSortOption.ALL,
    val sortAscending: Boolean = true,
    val viewMode: LibraryViewMode = LibraryViewMode.LIST,
    val isMultiSelectMode: Boolean = false,
    val selectedTrackIds: Set<String> = emptySet(),
    val openedAlbumKey: String? = null,
    val openedArtistName: String? = null,
    val openedPlaylistId: String? = null,
    val trackForActions: Track? = null,
    val showBatchAddToPlaylistDialog: Boolean = false,
    val showNewPlaylistDialog: Boolean = false
)

/**
 * ViewModel managing state and operations for LibraryScreen.
 */
class LibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSortOption(option: SongSortOption) {
        _uiState.update { current ->
            if (current.sortOption == option) {
                current.copy(sortAscending = !current.sortAscending)
            } else {
                current.copy(sortOption = option, sortAscending = true)
            }
        }
    }

    fun toggleSortAscending() {
        _uiState.update { it.copy(sortAscending = !it.sortAscending) }
    }

    fun setViewMode(mode: LibraryViewMode, settingsPreferences: SettingsPreferences? = null) {
        _uiState.update { it.copy(viewMode = mode) }
        settingsPreferences?.updateSettings { it.copy(libraryViewMode = mode.name) }
    }

    fun initViewMode(savedModeString: String) {
        val mode = try {
            LibraryViewMode.valueOf(savedModeString)
        } catch (_: Exception) {
            LibraryViewMode.LIST
        }
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun enterMultiSelect(initialTrackId: String? = null) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedTrackIds = if (initialTrackId != null) setOf(initialTrackId) else emptySet()
            )
        }
    }

    fun exitMultiSelect() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                selectedTrackIds = emptySet()
            )
        }
    }

    fun toggleMultiSelect() {
        _uiState.update { current ->
            val newMode = !current.isMultiSelectMode
            current.copy(
                isMultiSelectMode = newMode,
                selectedTrackIds = if (newMode) current.selectedTrackIds else emptySet()
            )
        }
    }

    fun toggleTrackSelection(trackId: String) {
        _uiState.update { current ->
            val updated = if (current.selectedTrackIds.contains(trackId)) {
                current.selectedTrackIds - trackId
            } else {
                current.selectedTrackIds + trackId
            }
            current.copy(selectedTrackIds = updated)
        }
    }

    fun selectAllTracks(allTrackIds: Collection<String>) {
        _uiState.update { current ->
            val newSelection = if (current.selectedTrackIds.size == allTrackIds.size) {
                emptySet()
            } else {
                allTrackIds.toSet()
            }
            current.copy(selectedTrackIds = newSelection)
        }
    }

    fun setSelectedTrackIds(ids: Set<String>) {
        _uiState.update { it.copy(selectedTrackIds = ids) }
    }

    fun openAlbum(albumKey: String) {
        _uiState.update { it.copy(openedAlbumKey = albumKey) }
    }

    fun closeAlbum() {
        _uiState.update { it.copy(openedAlbumKey = null) }
    }

    fun openArtist(artistName: String) {
        _uiState.update { it.copy(openedArtistName = artistName) }
    }

    fun closeArtist() {
        _uiState.update { it.copy(openedArtistName = null) }
    }

    fun openPlaylist(playlistId: String) {
        _uiState.update { it.copy(openedPlaylistId = playlistId) }
    }

    fun closePlaylist() {
        _uiState.update { it.copy(openedPlaylistId = null) }
    }

    fun closeAllDetails(): Boolean {
        val state = _uiState.value
        val hasDetail = state.openedAlbumKey != null || state.openedArtistName != null || state.openedPlaylistId != null
        if (hasDetail) {
            _uiState.update {
                it.copy(
                    openedAlbumKey = null,
                    openedArtistName = null,
                    openedPlaylistId = null
                )
            }
            return true
        }
        return false
    }

    fun setTrackForActions(track: Track?) {
        _uiState.update { it.copy(trackForActions = track) }
    }

    fun setShowBatchAddToPlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showBatchAddToPlaylistDialog = show) }
    }

    fun setShowNewPlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showNewPlaylistDialog = show) }
    }

    companion object {
        fun computeSortedTracks(
            tracks: List<Track>,
            sortOption: SongSortOption,
            sortAscending: Boolean
        ): List<Track> {
            return when (sortOption) {
                SongSortOption.ALL -> tracks
                SongSortOption.TITLE_AZ -> {
                    if (sortAscending) {
                        tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    } else {
                        tracks.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
                    }
                }
                SongSortOption.ARTIST -> {
                    if (sortAscending) {
                        tracks.sortedWith(
                            compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                        )
                    } else {
                        tracks.sortedWith(
                            compareByDescending<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                        )
                    }
                }
                SongSortOption.DURATION -> {
                    if (sortAscending) {
                        tracks.sortedBy { it.durationMs }
                    } else {
                        tracks.sortedByDescending { it.durationMs }
                    }
                }
            }
        }

        fun computeAvailableLetters(tracks: List<Track>): Set<Char> {
            return tracks.mapNotNull { it.title.trim().firstOrNull()?.uppercaseChar() }
                .map { if (it.isLetter()) it else '#' }
                .toSet()
        }
    }
}
