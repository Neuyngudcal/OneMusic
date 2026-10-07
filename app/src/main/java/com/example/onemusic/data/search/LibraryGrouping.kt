package com.example.onemusic.data.search

import com.example.onemusic.data.model.Track

/**
 * Một album sau khi gộp: khóa (xem [LibraryGrouping.albumKey]), tên hiển thị, nghệ sĩ đại diện, các bài.
 * Màn chi tiết lưu/tra album bằng [key] – KHÔNG bằng [name], vì 2 album khác nhau có thể trùng tên.
 */
data class AlbumGroup(
    val key: String,
    val name: String,
    val artist: String,
    val tracks: List<Track>
)

/**
 * Cách gộp nghệ sĩ / album DÙNG CHUNG cho Trang chủ, Thư viện và Tìm kiếm, để số lượng và
 * thành phần mỗi nhóm giống nhau ở mọi màn (trước đây mỗi màn gộp một kiểu → Home "120 nghệ sĩ",
 * Thư viện "134 nghệ sĩ"). Logic lấy theo Trang chủ (bản đúng hơn):
 * - Không phân biệt hoa thường ("Sơn Tùng" và "sơn tùng" là một), tên hiển thị = cách viết gặp đầu tiên.
 * - Chế độ gộp: tách bài hợp tác (feat, &, x...) cho từng nghệ sĩ rồi gộp tên chuẩn (mergeCanonicalArtists).
 * - Album gộp theo cặp (tên album, nghệ sĩ chính) → hai album "Greatest Hits" của 2 ca sĩ là 2 album riêng.
 *
 * Chưa làm: sắp xếp bài trong album theo số thứ tự track – `Track` chưa có trackNumber/discNumber.
 */
object LibraryGrouping {

    const val UNKNOWN_ARTIST = "Nghệ sĩ chưa rõ"
    const val UNKNOWN_ALBUM = "Album chưa rõ"

    /** Tách chuỗi nghệ sĩ thành từng nghệ sĩ riêng; chuỗi không tách được thì giữ nguyên. */
    fun artistNames(rawArtist: String): List<String> {
        val list = ArtistExtractor.extractIndividualArtists(rawArtist)
        return if (list.isEmpty()) listOf(rawArtist.ifBlank { UNKNOWN_ARTIST }) else list
    }

    /**
     * Gộp bài theo nghệ sĩ, sắp xếp A→Z.
     * @param mergeCollaborators true = tách bài hợp tác & gộp tên chuẩn (mặc định);
     *                           false = giữ nguyên chuỗi nghệ sĩ trong tệp.
     */
    fun groupArtists(tracks: List<Track>, mergeCollaborators: Boolean = true): List<Pair<String, List<Track>>> {
        if (!mergeCollaborators) {
            return groupCaseInsensitive(tracks) { listOf(it.artist.ifBlank { UNKNOWN_ARTIST }) }
                .sortedBy { it.first.lowercase() }
        }
        val rawMap = LinkedHashMap<String, MutableList<Track>>()
        val displayNameByLower = HashMap<String, String>()
        for (track in tracks) {
            for (name in artistNames(track.artist)) {
                val key = displayNameByLower.getOrPut(name.trim().lowercase()) { name }
                rawMap.getOrPut(key) { mutableListOf() }.add(track)
            }
        }
        return ArtistExtractor.mergeCanonicalArtists(rawMap)
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    /** Tên album hiển thị của bài (album trống → "Album chưa rõ"). */
    fun albumName(track: Track): String = track.album.trim().ifBlank { UNKNOWN_ALBUM }

    /**
     * Khóa album = "tên album|nghệ sĩ chính" (chữ thường, bỏ khoảng trắng hai đầu).
     * Nghệ sĩ chính = nghệ sĩ đầu tiên sau khi tách bài hợp tác, nên "A feat. B" vẫn cùng album với "A".
     */
    fun albumKey(track: Track): String {
        val album = albumName(track).lowercase()
        val artist = artistNames(track.artist).firstOrNull().orEmpty().trim().lowercase()
        return "$album|$artist"
    }

    /** Các bài thuộc album có khóa [key] (theo thứ tự trong [tracks]). */
    fun tracksOfAlbum(tracks: List<Track>, key: String): List<Track> =
        tracks.filter { albumKey(it) == key }

    /** Gộp bài theo (tên album, nghệ sĩ chính), sắp xếp A→Z theo tên album rồi tên nghệ sĩ. */
    fun groupAlbums(tracks: List<Track>): List<AlbumGroup> {
        val groups = LinkedHashMap<String, MutableList<Track>>()
        for (track in tracks) {
            groups.getOrPut(albumKey(track)) { mutableListOf() }.add(track)
        }
        return groups.map { (key, albumTracks) ->
            val first = albumTracks.first()
            AlbumGroup(
                key = key,
                name = albumName(first),
                artist = artistNames(first.artist).firstOrNull()?.trim()?.ifBlank { null } ?: UNKNOWN_ARTIST,
                tracks = albumTracks
            )
        }.sortedWith(compareBy<AlbumGroup>({ it.name.lowercase() }, { it.artist.lowercase() }))
    }

    private fun groupCaseInsensitive(
        tracks: List<Track>,
        keysOf: (Track) -> List<String>
    ): List<Pair<String, List<Track>>> {
        val groups = LinkedHashMap<String, Pair<String, MutableList<Track>>>()
        for (track in tracks) {
            for (name in keysOf(track)) {
                groups.getOrPut(name.trim().lowercase()) { name to mutableListOf() }.second.add(track)
            }
        }
        return groups.values.map { (name, list) -> name to list }
    }
}
