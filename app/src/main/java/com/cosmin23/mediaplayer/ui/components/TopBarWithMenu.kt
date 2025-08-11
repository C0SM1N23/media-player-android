package com.cosmin23.mediaplayer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.cosmin23.mediaplayer.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithMenu(
    title: String,
    navController: NavController,
    viewModel: PlayerViewModel
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Open menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(text = { Text("Media") }, onClick = {
                    menuExpanded = false
                    navController.navigate("player") { launchSingleTop = true }
                })
                DropdownMenuItem(text = { Text("Favorites") }, onClick = {
                    menuExpanded = false
                    navController.navigate("favorites") { launchSingleTop = true }
                })
                DropdownMenuItem(text = { Text("Equalizer") }, onClick = {
                    menuExpanded = false
                    navController.navigate("equalizer") { launchSingleTop = true }
                })
                DropdownMenuItem(text = { Text("Settings") }, onClick = {
                    menuExpanded = false
                    navController.navigate("settings") { launchSingleTop = true }
                })
                Divider()
                DropdownMenuItem(text = { Text(if (viewModel.darkMode.collectAsState().value) "Light mode" else "Dark mode") }, onClick = {
                    menuExpanded = false
                    viewModel.toggleDarkMode()
                })
            }
        }
    )
}
