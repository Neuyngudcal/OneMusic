package com.example.onemusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.onemusic.data.model.MotionArtworkEntity

/**
 * Room DAO for Motion Artwork cache operations.
 * All queries run on Dispatchers.IO via Room's suspend support.
 */
@Dao
interface MotionArtworkDao {

    @Query("SELECT * FROM motion_artwork WHERE trackId = :trackId")
    suspend fun getByTrackId(trackId: String): MotionArtworkEntity?

    @Query("SELECT * FROM motion_artwork WHERE appleAlbumId = :albumId AND hasMotion = 1 LIMIT 1")
    suspend fun getByAlbumId(albumId: String): MotionArtworkEntity?

    @Query("SELECT * FROM motion_artwork WHERE albumName = :albumName COLLATE NOCASE AND hasMotion = 1 AND isDownloaded = 1 LIMIT 1")
    suspend fun getByAlbumName(albumName: String): MotionArtworkEntity?

    @Query("SELECT * FROM motion_artwork WHERE artistName = :artistName COLLATE NOCASE AND albumName = :albumName COLLATE NOCASE AND hasMotion = 1 AND isDownloaded = 1 LIMIT 1")
    suspend fun getByArtistAndAlbum(artistName: String, albumName: String): MotionArtworkEntity?

    @Query("SELECT * FROM motion_artwork WHERE hasMotion = 1 AND isDownloaded = 1")
    suspend fun getAllDownloaded(): List<MotionArtworkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MotionArtworkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<MotionArtworkEntity>)

    @Query("DELETE FROM motion_artwork WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)

    @Query("SELECT COUNT(*) FROM motion_artwork WHERE hasMotion = 1")
    suspend fun countWithMotion(): Int

    @Query("SELECT COUNT(*) FROM motion_artwork")
    suspend fun countAll(): Int

    @Query("SELECT * FROM motion_artwork WHERE hasMotion = 1 AND isDownloaded = 0")
    suspend fun getPendingDownloads(): List<MotionArtworkEntity>

    @Query("SELECT * FROM motion_artwork WHERE hasMotion = 0 AND cachedAt < :expiryTimestamp")
    suspend fun getExpiredNegativeCache(expiryTimestamp: Long): List<MotionArtworkEntity>

    @Query("DELETE FROM motion_artwork WHERE hasMotion = 0 AND cachedAt < :expiryTimestamp")
    suspend fun clearExpiredNegativeCache(expiryTimestamp: Long)

    @Query("DELETE FROM motion_artwork WHERE appleAlbumId = :albumId")
    suspend fun deleteByAlbumId(albumId: String)

    @Query("DELETE FROM motion_artwork")
    suspend fun clearAll()
}
