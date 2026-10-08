package com.example.onemusic.playback.queue

import com.example.onemusic.data.model.Track
import kotlin.random.Random

/** Hàng đợi mới + vị trí bài đang phát trong hàng đợi đó. */
internal data class QueueResult(val queue: List<Track>, val currentIndex: Int)

/**
 * Thuật toán hàng đợi THUẦN KOTLIN (không ExoPlayer, không StateFlow) để unit test gọi thẳng.
 * [com.example.onemusic.playback.MusicPlayerController] tính kết quả ở đây rồi tự áp vào
 * `playbackState` và ExoPlayer. Hàm trả về `null` nghĩa là thao tác không hợp lệ → không làm gì.
 *
 * Shuffle kiểu Apple Music: hàng đợi luôn là THỨ TỰ PHÁT THẬT (đã xáo); "thứ tự gốc" (originalQueue)
 * chỉ dùng để khôi phục khi tắt shuffle.
 */
internal object QueueOperations {

    /** Vị trí chèn bài "Phát tiếp": ngay sau bài đang phát. */
    fun insertIndexAfterCurrent(queue: List<Track>, currentIndex: Int): Int =
        (currentIndex + 1).coerceIn(0, queue.size)

    /** Chèn [tracks] ngay sau bài đang phát; hàng đợi trống thì [tracks] thành hàng đợi mới. */
    fun insertNext(queue: List<Track>, currentIndex: Int, tracks: List<Track>): QueueResult {
        if (tracks.isEmpty()) return QueueResult(queue, currentIndex)
        if (queue.isEmpty()) return QueueResult(tracks, 0)
        val mutableQueue = queue.toMutableList()
        mutableQueue.addAll(insertIndexAfterCurrent(queue, currentIndex), tracks)
        return QueueResult(mutableQueue, currentIndex)
    }

    /** Nối [track] vào cuối hàng đợi; hàng đợi trống thì bắt đầu hàng đợi mới. */
    fun append(queue: List<Track>, currentIndex: Int, track: Track): QueueResult {
        if (queue.isEmpty()) return QueueResult(listOf(track), 0)
        return QueueResult(queue + track, currentIndex)
    }

    /** Kéo đổi chỗ một bài, giữ đúng bài đang phát. */
    fun move(queue: List<Track>, currentIndex: Int, fromIndex: Int, toIndex: Int): QueueResult? {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return null

        val mutableQueue = queue.toMutableList()
        val item = mutableQueue.removeAt(fromIndex)
        mutableQueue.add(toIndex, item)

        val newCurrentIndex = when {
            currentIndex == fromIndex -> toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        return QueueResult(mutableQueue, newCurrentIndex)
    }

    /**
     * Xóa bài ở [index]. Xóa bài đang phát thì bài kế tiếp (hoặc bài cuối) thành bài đang phát.
     * Xóa bài duy nhất → hàng đợi trống, `currentIndex = -1`.
     */
    fun remove(queue: List<Track>, currentIndex: Int, index: Int): QueueResult? {
        if (index !in queue.indices || queue.isEmpty()) return null
        if (queue.size == 1) return QueueResult(emptyList(), -1)

        val mutableQueue = queue.toMutableList()
        mutableQueue.removeAt(index)

        val newCurrentIndex = when {
            index < currentIndex -> currentIndex - 1
            index == currentIndex -> index.coerceAtMost(mutableQueue.lastIndex)
            else -> currentIndex
        }
        return QueueResult(mutableQueue, newCurrentIndex)
    }

    /** Xóa các bài đã phát (trước bài đang phát); bài đang phát thành bài đầu tiên. */
    fun clearHistory(queue: List<Track>, currentIndex: Int): QueueResult? {
        if (currentIndex <= 0 || queue.isEmpty()) return null
        return QueueResult(queue.subList(currentIndex, queue.size).toList(), 0)
    }

    /**
     * Bật shuffle: giữ nguyên lịch sử + bài đang phát, xáo phần phía sau.
     * Trả về `null` khi không có gì để xáo (bài đang phát là bài cuối / chỉ số không hợp lệ).
     */
    fun shuffleUpcoming(queue: List<Track>, currentIndex: Int, random: Random = Random): List<Track>? {
        if (currentIndex !in queue.indices || currentIndex + 1 >= queue.size) return null
        val head = queue.subList(0, currentIndex + 1)
        val tail = queue.subList(currentIndex + 1, queue.size).shuffled(random)
        return head + tail
    }

    /** Phát một danh sách mới khi đang shuffle: bài được chọn lên đầu, phần còn lại xáo ngẫu nhiên. */
    fun shuffledPlayOrder(tracks: List<Track>, startIndex: Int, random: Random = Random): List<Track> {
        val rest = tracks.filterIndexed { i, _ -> i != startIndex }.shuffled(random)
        return listOf(tracks[startIndex]) + rest
    }

    /**
     * Tắt shuffle: khôi phục [original], tìm lại bài đang phát theo id (không thấy thì giữ [currentIndex]).
     * Trả về `null` khi thứ tự gốc không hợp lệ (null / lệch số bài / hàng đợi trống) → giữ nguyên thứ tự hiện tại.
     */
    fun restoreOriginal(original: List<Track>?, queue: List<Track>, currentTrackId: String?, currentIndex: Int): QueueResult? {
        if (original == null || original.size != queue.size || queue.isEmpty()) return null
        val restoredIndex = original.indexOfFirst { it.id == currentTrackId }.takeIf { it >= 0 } ?: currentIndex
        return QueueResult(original.toList(), restoredIndex)
    }

    /**
     * Chuỗi lệnh `moveMediaItem(from, to)` để sắp danh sách media từ thứ tự [from] sang [to]
     * mà không ngắt bài đang phát. Bài có trong [to] nhưng không thấy trong [from] thì bỏ qua.
     */
    fun reorderMoves(from: List<Track>, to: List<Track>): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val working = from.toMutableList()
        for (i in to.indices) {
            if (working[i].id == to[i].id) continue
            var j = i + 1
            while (j < working.size && working[j].id != to[i].id) j++
            if (j >= working.size) continue
            moves += j to i
            working.add(i, working.removeAt(j))
        }
        return moves
    }

    /** Đang shuffle: bài "Phát tiếp" được đặt ngay sau bài đang phát trong thứ tự gốc. */
    fun insertAfterCurrentInOriginal(original: MutableList<Track>, tracks: List<Track>, currentTrackId: String?) {
        val currentPos = original.indexOfFirst { it.id == currentTrackId }
        val insertAt = if (currentPos >= 0) currentPos + 1 else original.size
        original.addAll(insertAt, tracks)
    }

    /** Đang shuffle: xóa 1 lần xuất hiện của bài khỏi thứ tự gốc để khi tắt shuffle không hiện lại. */
    fun removeFromOriginal(original: MutableList<Track>, track: Track) {
        val pos = original.indexOfFirst { it.id == track.id }
        if (pos >= 0) original.removeAt(pos)
    }
}
