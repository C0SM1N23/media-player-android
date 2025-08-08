package com.cosmin23.mediaplayer.data

import android.content.Context
import android.provider.MediaStore
import android.net.Uri

data class AudioItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val duration: Long
)

fun loadAudioFromMediaStore(context: Context): List<AudioItem> {
    val list = mutableListOf<AudioItem>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1 OR ${MediaStore.Audio.Media.IS_MUSIC} IS NULL"
    val sort = "${MediaStore.Audio.Media.TITLE} ASC"

    val query = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sort
    )

    query?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Unknown"
            val duration = cursor.getLong(durCol)
            val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
            list += AudioItem(id = id, uri = contentUri, title = title, duration = duration)
        }
    }
    return list
}
