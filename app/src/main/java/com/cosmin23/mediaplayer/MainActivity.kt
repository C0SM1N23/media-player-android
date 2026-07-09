package com.cosmin23.mediaplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cosmin23.mediaplayer.ui.EqualizerScreen
import com.cosmin23.mediaplayer.ui.HomeScreen
import com.cosmin23.mediaplayer.ui.LibraryScreen
import com.cosmin23.mediaplayer.ui.NowPlayingScreen
import com.cosmin23.mediaplayer.ui.PlaylistDetailScreen
import com.cosmin23.mediaplayer.ui.PlaylistsScreen
import com.cosmin23.mediaplayer.ui.SettingsScreen
import com.cosmin23.mediaplayer.ui.SongCollectionScreen
import com.cosmin23.mediaplayer.ui.components.MiniPlayer
import com.cosmin23.mediaplayer.ui.theme.MediaPlayerTheme

class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    private val readPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.loadLibrary()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestStoragePermissionIfNeeded()
        requestNotificationPermissionIfNeeded()

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            MediaPlayerTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                AppRoot(viewModel = vm)
            }
        }
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { vm.playExternalUri(it) }
    }

    private fun requestStoragePermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED) {
            vm.loadLibrary()
        } else {
            readPermissionLauncher.launch(permission)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PermissionChecker.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    LIBRARY("library", "Library", Icons.Filled.LibraryMusic),
    PLAYLISTS("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

@Composable
private fun AppRoot(viewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TopLevelDestination.HOME.route
    val snackbarHostState = remember { SnackbarHostState() }

    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    nowPlaying?.let { song ->
                        MiniPlayer(
                            item = song,
                            isPlaying = isPlaying,
                            progress = if (duration > 0) position.toFloat() / duration else 0f,
                            onPlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::next,
                            onClick = { navController.navigate("now_playing") }
                        )
                    }
                    BottomNavBar(currentRoute, navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 16 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 16 } }
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenFavorites = { navController.navigate("favorites") },
                    onOpenRecentlyPlayed = { navController.navigate("recently_played") },
                    onOpenMostPlayed = { navController.navigate("most_played") },
                    onOpenRecentlyAdded = { navController.navigate("recently_added") }
                )
            }
            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onOpenCollection = { navController.navigate("collection") }
                )
            }
            composable(TopLevelDestination.PLAYLISTS.route) {
                PlaylistsScreen(
                    viewModel = viewModel,
                    onOpenPlaylist = { id -> navController.navigate("playlist/$id") },
                    onOpenFavorites = { navController.navigate("favorites") },
                    onOpenRecentlyPlayed = { navController.navigate("recently_played") },
                    onOpenMostPlayed = { navController.navigate("most_played") },
                    onOpenRecentlyAdded = { navController.navigate("recently_added") }
                )
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(viewModel = viewModel, onOpenEqualizer = { navController.navigate("equalizer") })
            }

            composable("now_playing") {
                NowPlayingScreen(
                    viewModel = viewModel,
                    onCollapse = { navController.popBackStack() },
                    onOpenEqualizer = { navController.navigate("equalizer") }
                )
            }
            composable("equalizer") {
                EqualizerScaffold(onBack = { navController.popBackStack() }) {
                    EqualizerScreen(viewModel = viewModel)
                }
            }

            // Album / artist / folder snapshot collection
            composable("collection") {
                val collection by viewModel.openedCollection.collectAsStateWithLifecycle()
                val c = collection
                if (c == null) {
                    BackFallback(onBack = { navController.popBackStack() })
                } else {
                    SongCollectionScreen(
                        title = c.title, subtitle = c.subtitle, songs = c.songs,
                        viewModel = viewModel, onBack = { navController.popBackStack() }
                    )
                }
            }

            // Live smart playlists
            composable("favorites") {
                val songs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
                SongCollectionScreen(
                    title = "Favorites", subtitle = "${songs.size} songs", songs = songs,
                    viewModel = viewModel, onBack = { navController.popBackStack() }
                )
            }
            composable("recently_played") {
                val songs by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
                SongCollectionScreen(
                    title = "Recently played", subtitle = "${songs.size} songs", songs = songs,
                    viewModel = viewModel, onBack = { navController.popBackStack() }
                )
            }
            composable("most_played") {
                val songs by viewModel.mostPlayed.collectAsStateWithLifecycle()
                SongCollectionScreen(
                    title = "Most played", subtitle = "${songs.size} songs", songs = songs,
                    viewModel = viewModel, onBack = { navController.popBackStack() }
                )
            }
            composable("recently_added") {
                val songs by viewModel.recentlyAdded.collectAsStateWithLifecycle()
                SongCollectionScreen(
                    title = "Recently added", subtitle = "${songs.size} songs", songs = songs,
                    viewModel = viewModel, onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "playlist/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PlaylistDetailScreen(viewModel = viewModel, playlistId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomNavBar(currentRoute: String, navController: NavHostController) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun EqualizerScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding -> Column(modifier = Modifier.padding(padding)) { content() } }
}

@Composable
private fun BackFallback(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing to show", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
}
