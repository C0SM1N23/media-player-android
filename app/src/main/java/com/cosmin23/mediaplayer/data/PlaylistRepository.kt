package com.cosmin23.mediaplayer.data

import android.content.Context
import com.cosmin23.mediaplayer.data.db.AppDatabase
import com.cosmin23.mediaplayer.data.db.PlaylistEntity
import com.cosmin23.mediaplayer.data.db.PlaylistWithCount
import kotlinx.coroutines.flow.Flow

/** CRUD + membership operations for user playlists (backed by Room). */
class PlaylistRepository(context: Context) {

    private val dao = AppDatabase.get(context).playlistDao()

    val playlists: Flow<List<PlaylistWithCount>> = dao.playlistsWithCount()

    fun playlistName(playlistId: Long): Flow<PlaylistEntity?> = dao.playlist(playlistId)

    fun songIds(playlistId: Long): Flow<List<Long>> = dao.songIds(playlistId)

    suspend fun create(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Playlist" }, createdAt = System.currentTimeMillis()))

    suspend fun rename(playlistId: Long, name: String) =
        dao.rename(playlistId, name.trim().ifBlank { "Playlist" })

    suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun addSong(playlistId: Long, songId: Long) = dao.addSong(playlistId, songId)

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        songIds.forEach { dao.addSong(playlistId, it) }
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = dao.removeSong(playlistId, songId)

    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) =
        dao.reorder(playlistId, orderedSongIds)

    /** Creates a copy of a playlist (same songs, same order). Returns the new playlist id. */
    suspend fun duplicate(playlistId: Long, newName: String): Long {
        val ids = dao.songIdsNow(playlistId)
        val newId = create(newName)
        ids.forEach { dao.addSong(newId, it) }
        return newId
    }

    /** Snapshot of the ordered song ids, used when exporting to an .m3u file. */
    suspend fun songIdsSnapshot(playlistId: Long): List<Long> = dao.songIdsNow(playlistId)
}
