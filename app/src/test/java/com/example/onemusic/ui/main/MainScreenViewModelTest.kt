package com.example.onemusic.ui.main

import com.example.onemusic.data.model.Track
import com.example.onemusic.data.repository.MusicRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MusicRepositoryTest {
  @Test
  fun tracks_areEmptyByDefaultWithoutCache() = runTest {
    val repository = MusicRepository()
    val tracks = repository.tracks.first()
    assertTrue(tracks.isEmpty())
  }

  @Test
  fun isScanning_initialStateIsFalse() = runTest {
    val repository = MusicRepository()
    val isScanning = repository.isScanning.value
    assertEquals(false, isScanning)
  }

  @Test
  fun sortTracks_byTitle_sortsCorrectly() {
    val testTracks = listOf(
        Track(id = "1", title = "Zebra Beat", artist = "Artist A", album = "Album A", durationMs = 120000, audioUrl = "", artworkUrl = ""),
        Track(id = "2", title = "Alpha Sound", artist = "Artist B", album = "Album B", durationMs = 180000, audioUrl = "", artworkUrl = ""),
        Track(id = "3", title = "Moonlight", artist = "Artist C", album = "Album C", durationMs = 90000, audioUrl = "", artworkUrl = "")
    )
    val sortedAsc = testTracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    val sortedDesc = testTracks.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })

    assertEquals("Alpha Sound", sortedAsc.first().title)
    assertEquals("Zebra Beat", sortedDesc.first().title)
  }

  @Test
  fun sortTracks_byDuration_sortsCorrectly() {
    val testTracks = listOf(
        Track(id = "1", title = "Short Song", artist = "Artist A", album = "Album A", durationMs = 60000, audioUrl = "", artworkUrl = ""),
        Track(id = "2", title = "Long Song", artist = "Artist B", album = "Album B", durationMs = 300000, audioUrl = "", artworkUrl = ""),
        Track(id = "3", title = "Medium Song", artist = "Artist C", album = "Album C", durationMs = 180000, audioUrl = "", artworkUrl = "")
    )
    val sortedByDurationDesc = testTracks.sortedByDescending { it.durationMs }
    val sortedByDurationAsc = testTracks.sortedBy { it.durationMs }

    assertEquals("Long Song", sortedByDurationDesc.first().title)
    assertEquals("Short Song", sortedByDurationAsc.first().title)
  }
}

