package com.example.onemusic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.onemusic.data.model.MotionArtworkEntity

/**
 * OneMusic Room Database.
 * Currently hosts Motion Artwork cache table. Extensible for future tables.
 */
@Database(
    entities = [MotionArtworkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OneMusicDatabase : RoomDatabase() {

    abstract fun motionArtworkDao(): MotionArtworkDao

    companion object {
        @Volatile
        private var INSTANCE: OneMusicDatabase? = null

        fun getInstance(context: Context): OneMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OneMusicDatabase::class.java,
                    "onemusic_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
