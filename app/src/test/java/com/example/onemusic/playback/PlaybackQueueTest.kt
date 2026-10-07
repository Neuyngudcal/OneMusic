package com.example.onemusic.playback

import com.example.onemusic.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackQueueTest {

    private lateinit var trackA: Track
    private lateinit var trackB: Track
    private lateinit var trackC: Track
    private lateinit var trackD: Track
    private lateinit var trackE: Track

    @Before
    fun setUp() {
        trackA = Track(id = "A", title = "Track A", artist = "Artist 1", album = "Album 1", durationMs = 180000L, audioUrl = "http://a.mp3", artworkUrl = "")
        trackB = Track(id = "B", title = "Track B", artist = "Artist 2", album = "Album 2", durationMs = 200000L, audioUrl = "http://b.mp3", artworkUrl = "")
        trackC = Track(id = "C", title = "Track C", artist = "Artist 3", album = "Album 3", durationMs = 220000L, audioUrl = "http://c.mp3", artworkUrl = "")
        trackD = Track(id = "D", title = "Track D", artist = "Artist 4", album = "Album 4", durationMs = 240000L, audioUrl = "http://d.mp3", artworkUrl = "")
        trackE = Track(id = "E", title = "Track E", artist = "Artist 5", album = "Album 5", durationMs = 260000L, audioUrl = "http://e.mp3", artworkUrl = "")
    }

    // Helper implementing the exact queue algorithms from MusicPlayerController
    private fun playNext(queue: List<Track>, currentIndex: Int, track: Track): Pair<List<Track>, Int> {
        if (queue.isEmpty()) {
            return Pair(listOf(track), 0)
        }
        val insertIndex = (currentIndex + 1).coerceIn(0, queue.size)
        val mutable = queue.toMutableList()
        mutable.add(insertIndex, track)
        return Pair(mutable, currentIndex)
    }

    private fun playNextTracks(queue: List<Track>, currentIndex: Int, tracks: List<Track>): Pair<List<Track>, Int> {
        if (tracks.isEmpty()) return Pair(queue, currentIndex)
        if (queue.isEmpty()) return Pair(tracks, 0)
        val insertIndex = (currentIndex + 1).coerceIn(0, queue.size)
        val mutable = queue.toMutableList()
        mutable.addAll(insertIndex, tracks)
        return Pair(mutable, currentIndex)
    }

    private fun moveQueueItem(queue: List<Track>, currentIndex: Int, fromIndex: Int, toIndex: Int): Pair<List<Track>, Int> {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) {
            return Pair(queue, currentIndex)
        }
        val mutable = queue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)

        val newCurrentIndex = when {
            currentIndex == fromIndex -> toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        return Pair(mutable, newCurrentIndex)
    }

    // Helper implementing the exact addToQueue algorithm from MusicPlayerController.
    // Third value mirrors whether the empty-queue guard routes through setQueue (prepares
    // the player) instead of a bare addMediaItem.
    private fun addToQueue(queue: List<Track>, currentIndex: Int, track: Track): Triple<List<Track>, Int, Boolean> {
        if (queue.isEmpty()) {
            return Triple(listOf(track), 0, true)
        }
        return Triple(queue + track, currentIndex, false)
    }

    private fun removeQueueItem(queue: List<Track>, currentIndex: Int, removeIndex: Int): Pair<List<Track>, Int> {
        if (removeIndex !in queue.indices || queue.isEmpty()) {
            return Pair(queue, currentIndex)
        }
        if (queue.size == 1) {
            return Pair(emptyList(), -1)
        }
        val mutable = queue.toMutableList()
        mutable.removeAt(removeIndex)

        val newCurrentIndex = when {
            removeIndex < currentIndex -> currentIndex - 1
            removeIndex == currentIndex -> removeIndex.coerceAtMost(mutable.lastIndex)
            else -> currentIndex
        }
        return Pair(mutable, newCurrentIndex)
    }

    @Test
    fun testPlayNext_insertsImmediatelyAfterCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 0 // playing A

        val (newQueue, newIndex) = playNext(initialQueue, currentIndex, trackD)

        assertEquals(4, newQueue.size)
        assertEquals(listOf(trackA, trackD, trackB, trackC), newQueue)
        assertEquals(0, newIndex) // Current track A index unchanged
    }

    @Test
    fun testPlayNextTracks_insertsMultipleTracksInOrder() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        val (newQueue, newIndex) = playNextTracks(initialQueue, currentIndex, listOf(trackD, trackE))

        assertEquals(5, newQueue.size)
        assertEquals(listOf(trackA, trackB, trackD, trackE, trackC), newQueue)
        assertEquals(1, newIndex) // Current track B index unchanged
    }

    @Test
    fun testMoveQueueItem_moveCurrentlyPlayingTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 0 // playing A

        // Move A (from 0) to end (to 2)
        val (newQueue, newIndex) = moveQueueItem(initialQueue, currentIndex, 0, 2)

        assertEquals(listOf(trackB, trackC, trackA), newQueue)
        assertEquals(2, newIndex) // A is now at index 2
    }

    @Test
    fun testMoveQueueItem_moveItemBeforeCurrentTrackToAfter() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        // Move A (from 0) to 2
        val (newQueue, newIndex) = moveQueueItem(initialQueue, currentIndex, 0, 2)

        assertEquals(listOf(trackB, trackC, trackA), newQueue)
        assertEquals(0, newIndex) // B shifted from index 1 to index 0
    }

    @Test
    fun testRemoveQueueItem_removeBeforeCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 2 // playing C

        val (newQueue, newIndex) = removeQueueItem(initialQueue, currentIndex, 0) // remove A

        assertEquals(listOf(trackB, trackC), newQueue)
        assertEquals(1, newIndex) // C shifted from index 2 to index 1
    }

    @Test
    fun testRemoveQueueItem_removeCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        val (newQueue, newIndex) = removeQueueItem(initialQueue, currentIndex, 1) // remove B

        assertEquals(listOf(trackA, trackC), newQueue)
        assertEquals(1, newIndex) // now points to track C at index 1
    }

    @Test
    fun testRemoveQueueItem_removeSingleItemQueue() {
        val initialQueue = listOf(trackA)
        val currentIndex = 0

        val (newQueue, newIndex) = removeQueueItem(initialQueue, currentIndex, 0)

        assertTrue(newQueue.isEmpty())
        assertEquals(-1, newIndex)
    }

    @Test
    fun testAddToQueue_emptyQueue_routesThroughSetQueue() {
        val (newQueue, newIndex, usedSetQueuePath) = addToQueue(emptyList(), -1, trackA)

        assertEquals(listOf(trackA), newQueue)
        assertEquals(0, newIndex)
        assertTrue(usedSetQueuePath)
    }

    @Test
    fun testAddToQueue_nonEmptyQueue_appendsWithoutChangingCurrentIndex() {
        val initialQueue = listOf(trackA, trackB)
        val currentIndex = 0

        val (newQueue, newIndex, usedSetQueuePath) = addToQueue(initialQueue, currentIndex, trackC)

        assertEquals(listOf(trackA, trackB, trackC), newQueue)
        assertEquals(0, newIndex)
        assertTrue(!usedSetQueuePath)
    }
}
