package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cosmin23.mediaplayer.PlayerViewModel
import com.cosmin23.mediaplayer.ui.components.MusicListScreen
import com.cosmin23.mediaplayer.ui.components.PlayerControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Media Player") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )

            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                MusicListScreen(viewModel = viewModel)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                PlayerControls(viewModel = viewModel)
            }
        }
    )
}
