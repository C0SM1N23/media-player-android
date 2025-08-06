package com.cosmin23.mediaplayer

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmin23.mediaplayer.data.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = PlayerManager(application)

    // listă audio
    private val _audioList = MutableStateFlow<List<AudioItem>>(emptyList())
    val audioList: StateFlow<List<AudioItem>> = _audioList.asStateFlow()

    // fișierul care se redă
    private val _playingUri = MutableStateFlow<Uri?>(null)
    val playingUri: StateFlow<Uri?> = _playingUri.asStateFlow()

    // poziție curentă și durată
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        // încarcă lista de audio în background
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<AudioItem>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            )
            val cursor = application.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(projection[0])
                val titleIdx = it.getColumnIndexOrThrow(projection[1])
                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val title = it.getString(titleIdx)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list += AudioItem(id, title, uri)
                }
            }
            _audioList.value = list
        }
    }

    fun playItem(item: AudioItem) {
        manager.play(item.uri)
        _playingUri.value = item.uri
        _duration.value = manager.duration()

        // actualizează poziția periodic
        viewModelScope.launch {
            while (_playingUri.value == item.uri) {
                _position.value = manager.currentPosition()
                delay(500)
            }
        }
    }

    fun pause() = manager.pause()

    fun stop() {
        manager.stop()
        _playingUri.value = null
    }

    fun seekTo(ms: Long) {
        manager.seekTo(ms)
        _position.value = ms
    }

    override fun onCleared() {
        super.onCleared()
        manager.release()
    }
}
