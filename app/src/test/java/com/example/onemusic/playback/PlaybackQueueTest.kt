package com.example.onemusic.playback

import com.example.onemusic.data.model.Track
import com.example.onemusic.playback.queue.QueueOperations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/** Gọi thẳng [QueueOperations] – đúng thuật toán MusicPlayerController dùng khi chạy app. */
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

    @Test
    fun testPlayNext_insertsImmediatelyAfterCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 0 // playing A

        val (newQueue, newIndex) = QueueOperations.insertNext(initialQueue, currentIndex, listOf(trackD))

        assertEquals(4, newQueue.size)
        assertEquals(listOf(trackA, trackD, trackB, trackC), newQueue)
        assertEquals(0, newIndex) // Current track A index unchanged
        assertEquals(1, QueueOperations.insertIndexAfterCurrent(initialQueue, currentIndex))
    }

    @Test
    fun testPlayNextTracks_insertsMultipleTracksInOrder() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        val (newQueue, newIndex) = QueueOperations.insertNext(initialQueue, currentIndex, listOf(trackD, trackE))

        assertEquals(5, newQueue.size)
        assertEquals(listOf(trackA, trackB, trackD, trackE, trackC), newQueue)
        assertEquals(1, newIndex) // Current track B index unchanged
    }

    @Test
    fun testPlayNext_emptyQueue_startsNewQueue() {
        val (newQueue, newIndex) = QueueOperations.insertNext(emptyList(), -1, listOf(trackA))

        assertEquals(listOf(trackA), newQueue)
        assertEquals(0, newIndex)
    }

    @Test
    fun testPlayNextTracks_emptyTracks_keepsQueue() {
        val initialQueue = listOf(trackA, trackB)

        val (newQueue, newIndex) = QueueOperations.insertNext(initialQueue, 1, emptyList())

        assertEquals(initialQueue, newQueue)
        assertEquals(1, newIndex)
    }

    @Test
    fun testMoveQueueItem_moveCurrentlyPlayingTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 0 // playing A

        // Move A (from 0) to end (to 2)
        val (newQueue, newIndex) = QueueOperations.move(initialQueue, currentIndex, 0, 2)!!

        assertEquals(listOf(trackB, trackC, trackA), newQueue)
        assertEquals(2, newIndex) // A is now at index 2
    }

    @Test
    fun testMoveQueueItem_moveItemBeforeCurrentTrackToAfter() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        // Move A (from 0) to 2
        val (newQueue, newIndex) = QueueOperations.move(initialQueue, currentIndex, 0, 2)!!

        assertEquals(listOf(trackB, trackC, trackA), newQueue)
        assertEquals(0, newIndex) // B shifted from index 1 to index 0
    }

    @Test
    fun testMoveQueueItem_moveItemAfterCurrentTrackToBefore() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        // Move C (from 2) to 0
        val (newQueue, newIndex) = QueueOperations.move(initialQueue, currentIndex, 2, 0)!!

        assertEquals(listOf(trackC, trackA, trackB), newQueue)
        assertEquals(2, newIndex) // B shifted from index 1 to index 2
    }

    @Test
    fun testMoveQueueItem_invalidIndices_returnsNull() {
        val initialQueue = listOf(trackA, trackB, trackC)

        assertNull(QueueOperations.move(initialQueue, 0, 1, 1))
        assertNull(QueueOperations.move(initialQueue, 0, -1, 2))
        assertNull(QueueOperations.move(initialQueue, 0, 0, 3))
    }

    @Test
    fun testRemoveQueueItem_removeBeforeCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 2 // playing C

        val (newQueue, newIndex) = QueueOperations.remove(initialQueue, currentIndex, 0)!! // remove A

        assertEquals(listOf(trackB, trackC), newQueue)
        assertEquals(1, newIndex) // C shifted from index 2 to index 1
    }

    @Test
    fun testRemoveQueueItem_removeCurrentTrack() {
        val initialQueue = listOf(trackA, trackB, trackC)
        val currentIndex = 1 // playing B

        val (newQueue, newIndex) = QueueOperations.remove(initialQueue, currentIndex, 1)!! // remove B

        assertEquals(listOf(trackA, trackC), newQueue)
        assertEquals(1, newIndex) // now points to track C at index 1
    }

    @Test
    fun testRemoveQueueItem_removeCurrentLastTrack_pointsToNewLast() {
        val initialQueue = listOf(trackA, trackB, trackC)

        val (newQueue, newIndex) = QueueOperations.remove(initialQueue, 2, 2)!! // remove C (đang phát, bài cuối)

        assertEquals(listOf(trackA, trackB), newQueue)
        assertEquals(1, newIndex)
    }

    @Test
    fun testRemoveQueueItem_removeSingleItemQueue() {
        val initialQueue = listOf(trackA)
        val currentIndex = 0

        val (newQueue, newIndex) = QueueOperations.remove(initialQueue, currentIndex, 0)!!

        assertTrue(newQueue.isEmpty())
        assertEquals(-1, newIndex)
    }

    @Test
    fun testRemoveQueueItem_invalidIndex_returnsNull() {
        assertNull(QueueOperations.remove(listOf(trackA, trackB), 0, 5))
        assertNull(QueueOperations.remove(emptyList(), -1, 0))
    }

    @Test
    fun testAddToQueue_emptyQueue_startsNewQueue() {
        val (newQueue, newIndex) = QueueOperations.append(emptyList(), -1, trackA)

        assertEquals(listOf(trackA), newQueue)
        assertEquals(0, newIndex)
    }

    @Test
    fun testAddToQueue_nonEmptyQueue_appendsWithoutChangingCurrentIndex() {
        val initialQueue = listOf(trackA, trackB)
        val currentIndex = 0

        val (newQueue, newIndex) = QueueOperations.append(initialQueue, currentIndex, trackC)

        assertEquals(listOf(trackA, trackB, trackC), newQueue)
        assertEquals(0, newIndex)
    }

    @Test
    fun testClearHistory_keepsCurrentAndUpcoming() {
        val (newQueue, newIndex) = QueueOperations.clearHistory(listOf(trackA, trackB, trackC, trackD), 2)!!

        assertEquals(listOf(trackC, trackD), newQueue)
        assertEquals(0, newIndex)
    }

    @Test
    fun testClearHistory_nothingToClear_returnsNull() {
        assertNull(QueueOperations.clearHistory(listOf(trackA, trackB), 0))
        assertNull(QueueOperations.clearHistory(emptyList(), -1))
    }

    @Test
    fun testShuffleUpcoming_keepsHistoryAndCurrent_shufflesRest() {
        val queue = listOf(trackA, trackB, trackC, trackD, trackE)

        val shuffled = QueueOperations.shuffleUpcoming(queue, 1, Random(42))!!

        assertEquals(listOf(trackA, trackB), shuffled.subList(0, 2))
        assertEquals(setOf(trackC, trackD, trackE), shuffled.subList(2, 5).toSet())
        assertEquals(5, shuffled.size)
    }

    @Test
    fun testShuffleUpcoming_currentIsLast_returnsNull() {
        assertNull(QueueOperations.shuffleUpcoming(listOf(trackA, trackB), 1))
        assertNull(QueueOperations.shuffleUpcoming(emptyList(), -1))
    }

    @Test
    fun testShuffledPlayOrder_selectedTrackFirst() {
        val tracks = listOf(trackA, trackB, trackC, trackD)

        val order = QueueOperations.shuffledPlayOrder(tracks, 2, Random(7))

        assertEquals(trackC, order.first())
        assertEquals(tracks.toSet(), order.toSet())
        assertEquals(4, order.size)
    }

    @Test
    fun testRestoreOriginal_findsCurrentTrackById() {
        val original = listOf(trackA, trackB, trackC, trackD)
        val shuffled = listOf(trackA, trackD, trackB, trackC)

        val (restored, index) = QueueOperations.restoreOriginal(original, shuffled, "D", 1)!!

        assertEquals(original, restored)
        assertEquals(3, index)
    }

    @Test
    fun testRestoreOriginal_invalidOriginal_returnsNull() {
        val queue = listOf(trackA, trackB)

        assertNull(QueueOperations.restoreOriginal(null, queue, "A", 0))
        assertNull(QueueOperations.restoreOriginal(listOf(trackA), queue, "A", 0))
        assertNull(QueueOperations.restoreOriginal(emptyList(), emptyList(), null, -1))
    }

    @Test
    fun testReorderMoves_appliedInOrder_producesTargetOrder() {
        val from = listOf(trackA, trackB, trackC, trackD, trackE)
        val to = listOf(trackA, trackE, trackC, trackB, trackD)

        val working = from.toMutableList()
        for ((moveFrom, moveTo) in QueueOperations.reorderMoves(from, to)) {
            working.add(moveTo, working.removeAt(moveFrom)) // giống ExoPlayer.moveMediaItem
        }

        assertEquals(to, working)
    }

    @Test
    fun testReorderMoves_sameOrder_noMoves() {
        val queue = listOf(trackA, trackB, trackC)

        assertTrue(QueueOperations.reorderMoves(queue, queue).isEmpty())
    }

    @Test
    fun testOriginalQueue_insertAfterCurrentAndRemove() {
        val original = mutableListOf(trackA, trackB, trackC)

        QueueOperations.insertAfterCurrentInOriginal(original, listOf(trackD, trackE), "B")
        assertEquals(listOf(trackA, trackB, trackD, trackE, trackC), original)

        QueueOperations.removeFromOriginal(original, trackD)
        assertEquals(listOf(trackA, trackB, trackE, trackC), original)
    }

    @Test
    fun testOriginalQueue_insertWhenCurrentMissing_appendsToEnd() {
        val original = mutableListOf(trackA, trackB)

        QueueOperations.insertAfterCurrentInOriginal(original, listOf(trackC), null)

        assertEquals(listOf(trackA, trackB, trackC), original)
    }
}
