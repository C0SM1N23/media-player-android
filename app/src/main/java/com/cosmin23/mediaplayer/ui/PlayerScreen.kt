package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.data.AudioItem
import com.cosmin23.mediaplayer.ui.components.MusicListItem
import com.cosmin23.mediaplayer.ui.components.PlayerControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    // Colectăm listele și seturile din StateFlow ca state Compose
    val audioList = viewModel.audioList.collectAsState()
    val favorites = viewModel.favorites.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Media Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
            // Lista melodiilor
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(audioList.value) { audioItem: AudioItem ->
                    MusicListItem(
                        item = audioItem,
                        isFavorite = favorites.value.contains(audioItem.uri.toString()),
                        onPlay = { viewModel.playItem(audioItem) },
                        onToggleFavorite = { viewModel.toggleFavorite(audioItem.uri) }
                    )
                }
            }

            // Controalele playerului
            PlayerControls(viewModel = viewModel)
        }
    }
}
