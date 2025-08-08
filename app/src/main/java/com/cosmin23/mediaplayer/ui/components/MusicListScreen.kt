package com.cosmin23.mediaplayer.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem

@Composable
fun MusicListScreen(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val list = viewModel.audioList.collectAsState().value

    // picker folder (OpenDocumentTree)
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            // apelăm ViewModel care ia permisiune persistenta și încarcă fișiere
            viewModel.setFolderUriAndLoad(it)
        }
    }

    // picker fișier singular (OpenDocument)
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                viewModel.playUri(it)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Butoanele sus — sunt sub topAppBar (vezi PlayerScreen scaffold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { pickFolder.launch(null) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Choose folder", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose folder")
            }
            Button(onClick = { pickAudio.launch(arrayOf("audio/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.MusicNote, contentDescription = "Choose audio", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose audio")
            }
            Button(onClick = { viewModel.loadAudio() }, modifier = Modifier.weight(1f)) {
                Text("Refresh")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (list.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
            ) {
                Text(
                    "No audio files found.\nUse 'Choose folder' or add files to device Music/Download then press Refresh.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(list) { item: AudioItem ->
                    AudioRow(item = item, onClick = { viewModel.playItem(item) })
                }
            }
        }
    }
}

@Composable
private fun AudioRow(item: AudioItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDuration(item.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
