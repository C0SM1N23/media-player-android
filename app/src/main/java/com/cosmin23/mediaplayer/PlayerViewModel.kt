package com.cosmin23.mediaplayer

import android.app.Application
import android.content.ContentUris
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

    private var progressJob: Job? = null

    init {
        // încercăm MediaStore la start
        loadAudio()
    }

    /** Încarcă audio din MediaStore (implicit) */
    fun loadAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadAudioFromMediaStore(getApplication())
            _audioList.value = list
        }
    }

    /** Folosit când user a ales un folder cu OpenDocumentTree */
    fun setFolderUriAndLoad(treeUri: Uri) {
        // persistăm permisiunea ca să nu pierdem accesul (aceasta trebuie chemată din context cu flags)
        try {
            val cr = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            cr.takePersistableUriPermission(treeUri, takeFlags)
        } catch (t: Throwable) {
            // nu fatal, doar log (nu aruncăm)
            t.printStackTrace()
        }

        // încărcăm fișierele audio din tree
        viewModelScope.launch(Dispatchers.IO) {
            val list = loadAudioFromFolder(getApplication(), treeUri)
            _audioList.value = list
        }
    }

    /** Joacă un item din listă (folosește PlayerManager) */
    fun playItem(item: AudioItem) {
        manager.play(item.uri)
        _playingUri.value = item.uri
        _duration.value = if (item.duration > 0L) item.duration else manager.duration()
        startProgressUpdater(item.uri)
    }

    /** Joacă un URI (folosit pentru OpenDocument single file) */
    fun playUri(uri: Uri) {
        // persistăm permisiunea pentru documentul individual (dacă e permis)
        try {
            val cr = getApplication<Application>().contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            cr.takePersistableUriPermission(uri, takeFlags)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        manager.play(uri)
        _playingUri.value = uri

        val found = _audioList.value.find { it.uri == uri }
        _duration.value = found?.duration ?: manager.duration()
        startProgressUpdater(uri)
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
        _playingUri.value = uri
    }
    // în PlayerViewModel
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggleFavorite(uri: Uri) {
        val uriStr = uri.toString()
        _favorites.value = if (_favorites.value.contains(uriStr)) {
            _favorites.value - uriStr // șterge
        } else {
            _favorites.value + uriStr // adaugă
        }
    }



    // ---------- helper: încarcă fișiere audio dintr-un DocumentFile tree ----------
    private fun loadAudioFromFolder(app: Application, treeUri: Uri): List<AudioItem> {
        val ctx = app.applicationContext
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return emptyList()
        val list = mutableListOf<AudioItem>()

        // listare non-recursivă (poți extinde recursiv dacă vrei)
        val files = root.listFiles()
        for (f in files) {
            try {
                if (f.isFile) {
                    val mime = f.type ?: ""
                    if (mime.startsWith("audio") || f.name?.endsWith(".mp3", ignoreCase = true) == true
                        || f.name?.endsWith(".m4a", ignoreCase = true) == true
                        || f.name?.endsWith(".wav", ignoreCase = true) == true
                    ) {
                        val fileUri = f.uri
                        // extragem durata cu MediaMetadataRetriever (dacă se poate)
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
                        // generăm un id stabil-ish din uri
                        val id = fileUri.toString().hashCode().toLong()
                        list += AudioItem(id = id, uri = fileUri, title = title, duration = duration)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        return list.sortedBy { it.title.lowercase() }
    }
}
