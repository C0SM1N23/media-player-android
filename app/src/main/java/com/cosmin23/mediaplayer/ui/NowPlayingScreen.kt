package com.cosmin23.mediaplayer.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.data.artwork
import com.cosmin23.mediaplayer.ui.components.AlbumArt
import com.cosmin23.mediaplayer.ui.components.DraggableItem
import com.cosmin23.mediaplayer.ui.components.dragContainer
import com.cosmin23.mediaplayer.ui.components.rememberDragDropState
import com.cosmin23.mediaplayer.utils.formatDuration
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen player: palette-tinted background, large animated artwork, scrubbable seek bar, the
 * transport controls, plus favourite / queue / sleep-timer / playback-speed / equalizer actions.
 */
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepRemainingMs.collectAsStateWithLifecycle()
    val sleepAtEndOfTrack by viewModel.sleepAtEndOfTrack.collectAsStateWithLifecycle()

    val song = nowPlaying
    val artworkColor = rememberArtworkColor(song?.artwork, MaterialTheme.colorScheme.primaryContainer)
    val topColor by animateColorAsState(artworkColor.copy(alpha = 0.55f), label = "bgColor")

    var showQueue by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, MaterialTheme.colorScheme.background)))
            .padding(horizontal = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
            }
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showQueue = true }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
            }
        }

        if (song == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing is playing", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        Spacer(Modifier.height(16.dp))
        Crossfade(targetState = song, label = "artwork") { s ->
            AlbumArt(item = s, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 24)
        }

        Spacer(Modifier.height(28.dp))
        Text(song.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(song.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (song.album.isNotBlank()) {
            Text(song.album, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(20.dp))
        SeekBar(position = position, duration = duration, onSeek = viewModel::seekTo)

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = tintFor(shuffleEnabled))
            }
            IconButton(onClick = viewModel::previous, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onBackground)
            }
            FilledIconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.size(76.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = viewModel::next, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = viewModel::cycleRepeatMode) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = tintFor(repeatMode != Player.REPEAT_MODE_OFF)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            val isFavorite = song.id in favorites
            IconButton(onClick = { viewModel.toggleFavorite(song.id) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = tintFor(isFavorite)
                )
            }
            Box {
                IconButton(onClick = { showSpeedMenu = true }) {
                    Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = tintFor(speed != 1f))
                }
                SpeedMenu(expanded = showSpeedMenu, current = speed, onSelect = { viewModel.setPlaybackSpeed(it); showSpeedMenu = false }, onDismiss = { showSpeedMenu = false })
            }
            Box {
                IconButton(onClick = { showSleepMenu = true }) {
                    Icon(Icons.Filled.Bedtime, contentDescription = "Sleep timer", tint = tintFor(sleepRemaining > 0 || sleepAtEndOfTrack))
                }
                SleepMenu(
                    expanded = showSleepMenu,
                    remainingMs = sleepRemaining,
                    onPick = { minutes -> viewModel.startSleepTimer(minutes); showSleepMenu = false },
                    onEndOfTrack = { viewModel.startSleepTimerEndOfTrack(); showSleepMenu = false },
                    onCancel = { viewModel.cancelSleepTimer(); showSleepMenu = false },
                    onDismiss = { showSleepMenu = false }
                )
            }
            IconButton(onClick = onOpenEqualizer) {
                Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (sleepRemaining > 0) {
            Text(
                "Sleep in ${formatDuration(sleepRemaining)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showQueue) {
        QueueSheet(viewModel = viewModel, onDismiss = { showQueue = false })
    }
}

@Composable
private fun tintFor(active: Boolean): Color =
    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun SpeedMenu(expanded: Boolean, current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
            DropdownMenuItem(
                text = { Text("${value}x" + if (value == current) "  ✓" else "") },
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun SleepMenu(
    expanded: Boolean,
    remainingMs: Long,
    onPick: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(15, 30, 45, 60).forEach { minutes ->
            DropdownMenuItem(text = { Text("$minutes minutes") }, onClick = { onPick(minutes) })
        }
        DropdownMenuItem(text = { Text("End of track") }, onClick = onEndOfTrack)
        if (remainingMs > 0) {
            DropdownMenuItem(text = { Text("Cancel timer") }, onClick = onCancel)
        }
    }
}

@Composable
private fun QueueSheet(viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(listState) { from, to -> viewModel.moveQueueItem(from, to) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Up next", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.heightIn(max = 480.dp).dragContainer(dragState)
        ) {
            itemsIndexed(queue, key = { index, _ -> index }) { index, item ->
                DraggableItem(dragState, index) {
                    QueueRow(
                        item = item,
                        isCurrent = index == currentIndex,
                        onPlay = { viewModel.playQueueIndex(index) },
                        onRemove = { viewModel.removeFromQueue(index) }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QueueRow(item: AudioItem, isCurrent: Boolean, onPlay: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(item = item, modifier = Modifier.size(44.dp), cornerRadius = 8)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPlay) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play") }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
    }
}

@Composable
private fun SeekBar(position: Long, duration: Long, onSeek: (Long) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(position, isDragging) { if (!isDragging) sliderValue = position.toFloat() }

    val max = duration.coerceAtLeast(1L).toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue.coerceIn(0f, max),
            onValueChange = { isDragging = true; sliderValue = it },
            onValueChangeFinished = { onSeek(sliderValue.toLong()); isDragging = false },
            valueRange = 0f..max
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-${formatDuration((duration - sliderValue.toLong()).coerceAtLeast(0L))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Extracts a vibrant/dominant colour from the current artwork for the background gradient. */
@Composable
private fun rememberArtworkColor(model: Any?, fallback: Color): Color {
    val context = LocalContext.current
    var color by remember { mutableStateOf(fallback) }
    LaunchedEffect(model, fallback) {
        color = fallback
        if (model == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(model).allowHardware(false).size(200).build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable.let { it as? BitmapDrawable }?.bitmap
        if (bitmap != null) {
            val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
            (palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch)?.let {
                color = Color(it.rgb)
            }
        }
    }
    return color
}
