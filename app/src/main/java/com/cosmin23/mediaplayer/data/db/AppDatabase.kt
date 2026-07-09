package com.cosmin23.mediaplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local relational store for playback history and user playlists. Favorites and simple settings
 * stay in DataStore; only the relational data (play counts, ordered playlist membership) lives here.
 */
@Database(
    entities = [PlayStatEntity::class, PlaylistEntity::class, PlaylistItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playStatDao(): PlayStatDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_player.db"
                ).build().also { instance = it }
            }
    }
}
