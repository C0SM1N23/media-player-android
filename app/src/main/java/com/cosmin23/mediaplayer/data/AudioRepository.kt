package com.cosmin23.mediaplayer.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class AudioRepository(private val context: Context) {

    fun loadAudio(): List<AudioItem> {
        val audioList = mutableListOf<AudioItem>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val duration = cursor.getLong(durationColumn)
                val contentUri = Uri.withAppendedPath(uri, id.toString())

                // Folosim named args ca să nu depindem de ordinea din constructor
                audioList.add(
                    AudioItem(
                        id = id,
                        uri = contentUri,
                        title = title,
                        duration = duration
                    )
                )
            }
        }
        return audioList
    }
}
