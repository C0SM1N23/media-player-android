package com.cosmin23.mediaplayer

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cosmin23.mediaplayer.data.Album
import com.cosmin23.mediaplayer.data.Artist
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.AudioRepository
import com.cosmin23.mediaplayer.data.FavoritesRepository
import com.cosmin23.mediaplayer.data.Folder
import com.cosmin23.mediaplayer.data.HistoryRepository
import com.cosmin23.mediaplayer.data.M3uPlaylist
import com.cosmin23.mediaplayer.data.PlaylistRepository
import com.cosmin23.mediaplayer.data.SettingsRepository
import com.cosmin23.mediaplayer.data.SongCollection
import com.cosmin23.mediaplayer.data.SortOrder
import com.cosmin23.mediaplayer.data.ThemeMode
import com.cosmin23.mediaplayer.data.UserSettings
import com.cosmin23.mediaplayer.data.db.PlaylistWithCount
import com.cosmin23.mediaplayer.data.toAlbums
import com.cosmin23.mediaplayer.data.toArtists
import com.cosmin23.mediaplayer.data.toFolders
import com.cosmin23.mediaplayer.playback.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single ViewModel that owns UI state for the whole app and drives playback through one
 * [MediaController] connected to [MusicService]. Also fronts the repositories (library, favorites,
 * settings, play history, playlists) so the Compose layer stays free of Android/data details.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val playlistRepository = PlaylistRepository(application)

    private val eagerly = SharingStarted.Eagerly
    private val whileSubscribed = SharingStarted.WhileSubscribed(5_000)

    // --- Library --------------------------------------------------------------------------------
    private val _library = MutableStateFlow<List<AudioItem>>(emptyList())

    /** Fast id → track lookup, kept in sync with [_library] for queue/now-playing resolution. */
    @Volatile
    private var libraryIndex: Map<Long, AudioItem> = emptyMap()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingLibrary = MutableStateFlow(false)
    val isLoadingLibrary: StateFlow<Boolean> = _isLoadingLibrary.asStateFlow()

    val settings: StateFlow<UserSettings> =
        settingsRepository.settings.stateIn(viewModelScope, eagerly, UserSettings())

    val songs: StateFlow<List<AudioItem>> =
        combine(_library, _searchQuery, settingsRepository.settings) { library, query, settings ->
            val filtered = if (query.isBlank()) library else library.filter {
                it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
            }
            settings.sortOrder.sorted(filtered)
        }.stateIn(viewModelScope, whileSubscribed, emptyList())

    val albums: StateFlow<List<Album>> =
        _library.map { it.toAlbums() }.stateIn(viewModelScope, whileSubscribed, emptyList())
    val artists: StateFlow<List<Artist>> =
        _library.map { it.toArtists() }.stateIn(viewModelScope, whileSubscribed, emptyList())
    val folders: StateFlow<List<Folder>> =
        _library.map { it.toFolders() }.stateIn(viewModelScope, whileSubscribed, emptyList())

    // --- Favorites & smart playlists ------------------------------------------------------------
    val favoriteIds: StateFlow<Set<Long>> =
        favoritesRepository.favoriteIds.stateIn(viewModelScope, eagerly, emptySet())

    val favoriteSongs: StateFlow<List<AudioItem>> =
        combine(_library, favoritesRepository.favoriteIds) { library, favorites ->
            library.filter { it.id in favorites }
        }.stateIn(viewModelScope, whileSubscribed, emptyList())

    val recentlyAdded: StateFlow<List<AudioItem>> =
        _library.map { lib -> lib.sortedByDescending { it.dateAddedSeconds }.take(100) }
            .stateIn(viewModelScope, whileSubscribed, emptyList())

    val recentlyPlayed: StateFlow<List<AudioItem>> =
        combine(_library, historyRepository.recentlyPlayedIds()) { lib, ids -> lib.pick(ids) }
            .stateIn(viewModelScope, whileSubscribed, emptyList())

    val mostPlayed: StateFlow<List<AudioItem>> =
        combine(_library, historyRepository.mostPlayedIds()) { lib, ids -> lib.pick(ids) }
            .stateIn(viewModelScope, whileSubscribed, emptyList())

    // --- User playlists -------------------------------------------------------------------------
    val playlists: StateFlow<List<PlaylistWithCount>> =
        playlistRepository.playlists.stateIn(viewModelScope, whileSubscribed, emptyList())

    fun playlistSongs(playlistId: Long): Flow<List<AudioItem>> =
        combine(_library, playlistRepository.songIds(playlistId)) { lib, ids -> lib.pick(ids) }

    fun playlistName(playlistId: Long): Flow<String> =
        playlistRepository.playlistName(playlistId).map { it?.name ?: "Playlist" }

    // --- Playback state -------------------------------------------------------------------------
    private val _nowPlaying = MutableStateFlow<AudioItem?>(null)
    val nowPlaying: StateFlow<AudioItem?> = _nowPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioItem>>(emptyList())
    val queue: StateFlow<List<AudioItem>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    /** A snapshot collection (album / artist / folder) opened in the detail screen. */
    private val _openedCollection = MutableStateFlow<SongCollection?>(null)
    val openedCollection: StateFlow<SongCollection?> = _openedCollection.asStateFlow()

    fun openCollection(title: String, subtitle: String, songs: List<AudioItem>) {
        _openedCollection.value = SongCollection(title, subtitle, songs)
    }

    // --- Sleep timer ----------------------------------------------------------------------------
    private val _sleepRemainingMs = MutableStateFlow(0L)
    val sleepRemainingMs: StateFlow<Long> = _sleepRemainingMs.asStateFlow()

    private val _sleepAtEndOfTrack = MutableStateFlow(false)
    val sleepAtEndOfTrack: StateFlow<Boolean> = _sleepAtEndOfTrack.asStateFlow()

    private var sleepJob: Job? = null
    private var sleepEndOfTrackMediaId: Long? = null

    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null
    private var lastRecordedId: Long? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromController()
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) rebuildQueue()
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) onTrackChanged()
        }
    }

    init {
        connectToService()
        loadLibrary()
    }

    // --- Service connection ---------------------------------------------------------------------
    private fun connectToService() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { c ->
                controller = c
                c.addListener(playerListener)
                syncFromController()
                rebuildQueue()
                startProgressUpdater()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun syncFromController() {
        val c = controller ?: return
        _isPlaying.value = c.isPlaying
        _shuffleEnabled.value = c.shuffleModeEnabled
        _repeatMode.value = c.repeatMode
        _duration.value = c.duration.coerceAtLeast(0L)
        _position.value = c.currentPosition.coerceAtLeast(0L)
        _playbackSpeed.value = c.playbackParameters.speed
        _currentQueueIndex.value = c.currentMediaItemIndex
        _nowPlaying.value = c.currentMediaItem?.let(::resolveTrack)
        c.sessionExtras.getInt(MusicService.EXTRA_AUDIO_SESSION_ID, 0)
            .takeIf { it > 0 }?.let { _audioSessionId.value = it }
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        _queue.value = (0 until c.mediaItemCount).map { resolveTrack(c.getMediaItemAt(it)) }
        _currentQueueIndex.value = c.currentMediaItemIndex
    }

    private fun onTrackChanged() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
        if (id != null && id != lastRecordedId) {
            lastRecordedId = id
            viewModelScope.launch { historyRepository.recordPlay(id) }
        }
        if (_sleepAtEndOfTrack.value && id != sleepEndOfTrackMediaId) {
            c.pause()
            cancelSleepTimer()
        }
    }

    private fun resolveTrack(item: MediaItem): AudioItem {
        item.mediaId.toLongOrNull()?.let { id -> libraryIndex[id]?.let { return it } }
        val meta = item.mediaMetadata
        val uri = item.localConfiguration?.uri ?: Uri.EMPTY
        return AudioItem(
            id = item.mediaId.toLongOrNull() ?: uri.toString().hashCode().toLong(),
            uri = uri,
            title = meta.title?.toString() ?: uri.lastPathSegment ?: "Unknown",
            duration = controller?.duration?.coerceAtLeast(0L) ?: 0L,
            artist = meta.artist?.toString() ?: AudioItem.UNKNOWN_ARTIST,
            album = meta.albumTitle?.toString() ?: AudioItem.UNKNOWN_ALBUM,
            albumArtUri = meta.artworkUri
        )
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null) {
                    if (c.isPlaying) {
                        _position.value = c.currentPosition.coerceAtLeast(0L)
                        if (_duration.value <= 0L) _duration.value = c.duration.coerceAtLeast(0L)
                    }
                    c.sessionExtras.getInt(MusicService.EXTRA_AUDIO_SESSION_ID, 0)
                        .takeIf { it > 0 }?.let { _audioSessionId.value = it }
                }
                delay(500)
            }
        }
    }

    // --- Library actions ------------------------------------------------------------------------
    fun loadLibrary() {
        viewModelScope.launch {
            _isLoadingLibrary.value = true
            setLibrary(withContext(Dispatchers.IO) { audioRepository.loadFromMediaStore() })
            _isLoadingLibrary.value = false
        }
    }

    fun setFolderUriAndLoad(treeUri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModelScope.launch {
            _isLoadingLibrary.value = true
            setLibrary(withContext(Dispatchers.IO) { audioRepository.loadFromFolder(treeUri) })
            _isLoadingLibrary.value = false
        }
    }

    private fun setLibrary(list: List<AudioItem>) {
        libraryIndex = list.associateBy { it.id }
        _library.value = list
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(order) }
    }

    // --- Playback actions -----------------------------------------------------------------------
    fun play(items: List<AudioItem>, startIndex: Int) {
        val c = controller ?: return
        if (items.isEmpty() || startIndex !in items.indices) return
        if (c.shuffleModeEnabled) c.shuffleModeEnabled = false
        c.setMediaItems(items.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    /** Plays a collection in a fresh shuffled order. */
    fun shufflePlay(items: List<AudioItem>) {
        val c = controller ?: return
        if (items.isEmpty()) return
        c.setMediaItems(items.map { it.toMediaItem() }, items.indices.random(), 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_IDLE -> { c.prepare(); c.play() }
            else -> c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _position.value = positionMs
    }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val c = controller ?: return
        c.playbackParameters = PlaybackParameters(speed)
        _playbackSpeed.value = speed
    }

    fun playExternalUri(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val c = controller ?: return
        val mediaItem = _library.value.firstOrNull { it.uri == uri }?.toMediaItem()
            ?: MediaItem.Builder()
                .setMediaId(uri.toString().hashCode().toString())
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(uri.lastPathSegment ?: "Unknown").build())
                .build()
        c.setMediaItem(mediaItem)
        c.prepare()
        c.play()
    }

    // --- Queue management -----------------------------------------------------------------------
    fun playNext(item: AudioItem) {
        val c = controller ?: return
        val wasEmpty = c.mediaItemCount == 0
        val index = (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount)
        c.addMediaItem(index, item.toMediaItem())
        if (wasEmpty) { c.prepare(); c.play() }
    }

    fun addToQueue(item: AudioItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<AudioItem>) {
        val c = controller ?: return
        if (items.isEmpty()) return
        val wasEmpty = c.mediaItemCount == 0
        c.addMediaItems(items.map { it.toMediaItem() })
        if (wasEmpty) { c.prepare(); c.play() }
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount && from != to) {
            c.moveMediaItem(from, to)
        }
    }

    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
        }
    }

    // --- Sleep timer ----------------------------------------------------------------------------
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMs = minutes * 60_000L
        _sleepRemainingMs.value = totalMs
        sleepJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1_000
                _sleepRemainingMs.value = remaining.coerceAtLeast(0)
            }
            controller?.pause()
            _sleepRemainingMs.value = 0
        }
    }

    fun startSleepTimerEndOfTrack() {
        cancelSleepTimer()
        sleepEndOfTrackMediaId = controller?.currentMediaItem?.mediaId?.toLongOrNull()
        _sleepAtEndOfTrack.value = true
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndOfTrackMediaId = null
        _sleepRemainingMs.value = 0
        _sleepAtEndOfTrack.value = false
    }

    // --- Favorites / theme ----------------------------------------------------------------------
    fun toggleFavorite(id: Long) {
        viewModelScope.launch { favoritesRepository.toggle(id) }
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settingsRepository.setThemeMode(mode) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { settingsRepository.setDynamicColor(enabled) } }

    // --- Playlist actions -----------------------------------------------------------------------
    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = playlistRepository.create(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: Long, name: String) { viewModelScope.launch { playlistRepository.rename(id, name) } }
    fun deletePlaylist(id: Long) { viewModelScope.launch { playlistRepository.delete(id) } }
    fun duplicatePlaylist(id: Long, name: String) { viewModelScope.launch { playlistRepository.duplicate(id, name) } }
    fun addSongToPlaylist(playlistId: Long, songId: Long) { viewModelScope.launch { playlistRepository.addSong(playlistId, songId) } }
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) { viewModelScope.launch { playlistRepository.removeSong(playlistId, songId) } }
    fun reorderPlaylist(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch { playlistRepository.reorder(playlistId, orderedSongIds) }
    }

    // --- Playlist import / export ---------------------------------------------------------------
    fun exportPlaylistM3u(playlistId: Long, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val byId = libraryIndex
            val entries = playlistRepository.songIdsSnapshot(playlistId)
                .mapNotNull { byId[it] }
                .map {
                    M3uPlaylist.M3uEntry(
                        reference = it.displayName.ifBlank { it.title },
                        title = it.title,
                        artist = it.artist,
                        durationMs = it.duration
                    )
                }
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use {
                    it.write(M3uPlaylist.export(entries).toByteArray())
                }
            }
        }
    }

    fun importPlaylistM3u(sourceUri: Uri, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = runCatching {
                getApplication<Application>().contentResolver.openInputStream(sourceUri)?.use {
                    it.readBytes().decodeToString()
                }
            }.getOrNull() ?: return@launch
            val byName = _library.value.associateBy { it.displayName.lowercase() }
            val ids = M3uPlaylist.parseReferences(text)
                .mapNotNull { byName[M3uPlaylist.fileNameOf(it).lowercase()]?.id }
            if (ids.isNotEmpty()) {
                val playlistId = playlistRepository.create(name)
                playlistRepository.addSongs(playlistId, ids)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        sleepJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    private fun List<AudioItem>.pick(ids: List<Long>): List<AudioItem> {
        val byId = associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    private fun AudioItem.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
}
