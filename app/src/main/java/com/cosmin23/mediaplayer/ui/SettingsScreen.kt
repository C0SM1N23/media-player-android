package com.cosmin23.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null,
    onBack: (() -> Unit)? = null,
    onMusicListClicked: (() -> Unit)? = null,
    onEqualizerClicked: (() -> Unit)? = null,
    onFavoritesClicked: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // folosim CenterAlignedTopAppBar pentru compatibilitate largă
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController != null) navController.popBackStack() else onBack?.invoke()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Music list") },
                            onClick = {
                                menuExpanded = false
                                onMusicListClicked?.invoke() ?: navController?.navigate("music_list")
                            },
                            leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Equalizer") },
                            onClick = {
                                menuExpanded = false
                                onEqualizerClicked?.invoke() ?: navController?.navigate("equalizer")
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Favorites") },
                            onClick = {
                                menuExpanded = false
                                onFavoritesClicked?.invoke() ?: navController?.navigate("favorites")
                            },
                            leadingIcon = { Icon(Icons.Outlined.Favorite, contentDescription = null) }
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                        Text("Enable dark theme for the app", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Playback", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear cache", style = MaterialTheme.typography.bodyLarge)
                        Text("Remove cached audio data", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { /* cleanup logic */ }) {
                        Text("Clear")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text("About", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("MediaPlayerApp — simple player", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("Version: 1.0", style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}
