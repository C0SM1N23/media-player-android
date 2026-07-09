package com.cosmin23.mediaplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        "SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt, " +
            "(SELECT COUNT(*) FROM playlist_items pi WHERE pi.playlistId = p.id) AS songCount " +
            "FROM playlists p ORDER BY p.createdAt DESC"
    )
    fun playlistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun playlist(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT songId FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun songIds(playlistId: Long): Flow<List<Long>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert
    suspend fun insertItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: Long)

    @Query("SELECT songId FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun songIdsNow(playlistId: Long): List<Long>

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearItems(playlistId: Long)

    /** Appends a song to the end of a playlist. */
    suspend fun addSong(playlistId: Long, songId: Long) {
        insertItem(PlaylistItemEntity(playlistId = playlistId, songId = songId, position = maxPosition(playlistId) + 1))
    }

    /** Replaces the full ordering of a playlist (used after drag-to-reorder). */
    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) {
        clearItems(playlistId)
        orderedSongIds.forEachIndexed { index, songId ->
            insertItem(PlaylistItemEntity(playlistId = playlistId, songId = songId, position = index))
        }
    }
}
