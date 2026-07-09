package com.cosmin23.mediaplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayStatDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: PlayStatEntity)

    @Query("UPDATE play_stats SET playCount = playCount + 1, lastPlayedAt = :now WHERE songId = :songId")
    suspend fun increment(songId: Long, now: Long)

    /**
     * Records a play by ensuring the row exists (count 0) then incrementing it. Uses two portable
     * statements instead of SQLite UPSERT, which is unavailable before Android 11 (SQLite < 3.24).
     */
    suspend fun recordPlay(songId: Long, now: Long) {
        insertIfAbsent(PlayStatEntity(songId = songId, playCount = 0, lastPlayedAt = now))
        increment(songId, now)
    }

    @Query("SELECT songId FROM play_stats ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun mostPlayedIds(limit: Int): Flow<List<Long>>

    @Query("SELECT songId FROM play_stats ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recentlyPlayedIds(limit: Int): Flow<List<Long>>
}
