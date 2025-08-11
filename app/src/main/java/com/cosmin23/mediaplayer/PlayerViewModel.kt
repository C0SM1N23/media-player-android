package com.cosmin23.mediaplayer

import android.app.Application
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.loadAudioFromMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = PlayerManager(application)

    private val _audioList = MutableStateFlow<List<AudioItem>>(emptyList())
    val audioList: StateFlow<List<AudioItem>> = _audioList.asStateFlow()

    private val _playingUri = MutableStateFlow<Uri?>(null)
    val playingUri: StateFlow<Uri?> = _playingUri.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // audio session id -> folosit de equalizer
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    // dark mode toggle
    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private var progressJob: Job? = null

    // favorites (uri strings)
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    init {
        // try to load audio list at start
        loadAudio()
    }

    fun toggleDarkMode() {
        _darkMode.value = !_darkMode.value
    }

    fun setDarkMode(value: Boolean) {
        _darkMode.value = value
    }

    fun loadAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadAudioFromMediaStore(getApplication())
            _audioList.value = list
        }
    }

    fun setFolderUriAndLoad(treeUri: Uri) {
        try {
            val cr = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            cr.takePersistableUriPermission(treeUri, takeFlags)
        } catch (t: Throwable) { t.printStackTrace() }

        viewModelScope.launch(Dispatchers.IO) {
            val list = loadAudioFromFolder(getApplication(), treeUri)
            _audioList.value = list
        }
    }

    /**
     * Poll until audioSessionId > 0 or timeout.
     * This is necessary because ExoPlayer may create the audio session a short moment after prepare/play.
     */

    private fun updateAudioSessionIdEventually() {
        viewModelScope.launch {
            repeat(25) { // ~25 * 200ms = ~5s total retry window
                val id = manager.audioSessionId()
                if (id > 0) {
                    _audioSessionId.value = id
                    return@launch
                }
                delay(200)
            }
            // if still 0, leave as 0 (UI will show unavailable)
        }
    }

    fun playItem(item: AudioItem) {
        manager.play(item.uri)
        _playingUri.value = item.uri
        _duration.value = if (item.duration > 0L) item.duration else manager.duration()
        startProgressUpdater(item.uri)
        updateAudioSessionIdEventually()
    }

    fun playUri(uri: Uri) {
        try {
            val cr = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            cr.takePersistableUriPermission(uri, takeFlags)
        } catch (t: Throwable) { t.printStackTrace() }

        manager.play(uri)
        _playingUri.value = uri
        val found = _audioList.value.find { it.uri == uri }
        _duration.value = found?.duration ?: manager.duration()
        startProgressUpdater(uri)
        updateAudioSessionIdEventually()
    }

    private fun startProgressUpdater(uri: Uri) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_playingUri.value == uri) {
                _position.value = manager.currentPosition()
                delay(400)
            }
        }
    }

    fun pause() = manager.pause()

    fun stop() {
        manager.stop()
        _playingUri.value = null
        _position.value = 0L
        progressJob?.cancel()
        _audioSessionId.value = 0
    }

    fun seekTo(ms: Long) {
        manager.seekTo(ms)
        _position.value = ms
    }

    override fun onCleared() {
        super.onCleared()
        manager.release()
        progressJob?.cancel()
    }

    fun playUriFromExternal(uri: Uri) {
        // prefer internal playUri so audioSession is set and permission persisted
        playUri(uri)
    }

    // --- favorites
    fun toggleFavorite(uri: Uri) {
        val uriStr = uri.toString()
        _favorites.value = if (_favorites.value.contains(uriStr)) {
            _favorites.value - uriStr
        } else {
            _favorites.value + uriStr
        }
    }

    // --- next / previous helpers (simple)
    fun next() {
        val list = _audioList.value
        val current = _playingUri.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.uri == current }
        val nextIdx = if (idx == -1 || idx + 1 >= list.size) 0 else idx + 1
        playItem(list[nextIdx])
    }

    fun previous() {
        val list = _audioList.value
        val current = _playingUri.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.uri == current }
        val prevIdx = if (idx <= 0) list.size - 1 else idx - 1
        playItem(list[prevIdx])
    }

    // ---------- helper: loadAudioFromFolder kept as before ----------
    private fun loadAudioFromFolder(app: Application, treeUri: Uri): List<AudioItem> {
        val ctx = app.applicationContext
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return emptyList()
        val list = mutableListOf<AudioItem>()
        val files = root.listFiles()
        for (f in files) {
            try {
                if (f.isFile) {
                    val mime = f.type ?: ""
                    if (mime.startsWith("audio") || f.name?.endsWith(".mp3", true) == true
                        || f.name?.endsWith(".m4a", true) == true
                        || f.name?.endsWith(".wav", true) == true
                    ) {
                        val fileUri = f.uri
                        var duration = 0L
                        try {
                            val mmr = MediaMetadataRetriever()
                            val pfd = ctx.contentResolver.openFileDescriptor(fileUri, "r")
                            pfd?.use {
                                mmr.setDataSource(it.fileDescriptor)
                                val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                duration = dur?.toLongOrNull() ?: 0L
                                mmr.release()
                            }
                        } catch (t: Throwable) {
                            duration = 0L
                        }
                        val title = f.name ?: fileUri.lastPathSegment ?: "Unknown"
                        val id = fileUri.toString().hashCode().toLong()
                        list += AudioItem(id = id, uri = fileUri, title = title, duration = duration)
                    }
                }
            } catch (t: Throwable) { t.printStackTrace() }
        }
        return list.sortedBy { it.title.lowercase() }
    }
}
