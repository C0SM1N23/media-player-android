package com.cosmin23.mediaplayer.data

import android.content.Context
import com.cosmin23.mediaplayer.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow

/** Records plays and exposes the id lists backing the "Most played" / "Recently played" playlists. */
class HistoryRepository(context: Context) {

    private val dao = AppDatabase.get(context).playStatDao()

    fun mostPlayedIds(limit: Int = 100): Flow<List<Long>> = dao.mostPlayedIds(limit)

    fun recentlyPlayedIds(limit: Int = 100): Flow<List<Long>> = dao.recentlyPlayedIds(limit)

    suspend fun recordPlay(songId: Long) = dao.recordPlay(songId, System.currentTimeMillis())
}
