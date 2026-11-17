package com.xplayer.media.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xplayer.media.domain.model.LibraryUiState
import com.xplayer.media.domain.model.LocalMediaItem
import com.xplayer.media.domain.model.MediaType
import com.xplayer.media.domain.model.SortOption
import com.xplayer.media.ui.components.BottomMiniPlayer
import com.xplayer.media.ui.components.EmptyLibraryState
import com.xplayer.media.ui.components.LoadingLibraryState
import com.xplayer.media.ui.components.MediaItemRow
import com.xplayer.media.ui.components.PermissionGateCard
import com.xplayer.media.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    onOpenExternalFile: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleOrientation: () -> Unit
) {
    val libraryState by viewModel.libraryState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val userPrefs by viewModel.userPrefs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var mediaItemForPlaylist by remember { mutableStateOf<LocalMediaItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    if (isPlayerExpanded && playbackState.currentMedia != null) {
        NowPlayingScreen(
            playbackState = playbackState,
            mediaController = viewModel.getMediaController(),
            onClose = { isPlayerExpanded = false },
            onTogglePlayPause = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onSeekRelative = viewModel::seekRelative,
            onSkipNext = viewModel::skipNext,
            onSkipPrevious = viewModel::skipPrevious,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onToggleShuffle = viewModel::toggleShuffle,
            onSetSpeed = viewModel::setPlaybackSpeed,
            onSetSleepTimer = viewModel::setSleepTimer,
            onEnterPip = onEnterPip,
            onToggleOrientation = onToggleOrientation
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("X Player", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            viewModel.onSortOptionSelected(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onOpenExternalFile) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Open File")
                        }
                        IconButton(onClick = viewModel::refreshLibrary) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan Library")
                        }
                    }
                )
                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search songs, artists, albums, videos…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        },
        bottomBar = {
            Column {
                BottomMiniPlayer(
                    playbackState = playbackState,
                    onExpandPlayer = { isPlayerExpanded = true },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipNext = viewModel::skipNext
                )
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AudioFile, contentDescription = null) },
                        label = { Text("Songs") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        label = { Text("Videos") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        label = { Text("Playlists") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text("Folders") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                4 -> SettingsTabContent(
                    userPrefs = userPrefs,
                    onDarkThemeChanged = viewModel::setDarkThemeMode,
                    onDynamicColorChanged = viewModel::setDynamicColors,
                    onResumeVideoChanged = viewModel::setResumeVideoPlayback,
                    onAutoSkipCorruptedChanged = viewModel::setAutoSkipCorrupted,
                    onClearHistory = viewModel::clearHistory
                )
                else -> {
                    when (val state = libraryState) {
                        is LibraryUiState.PermissionRequired -> PermissionGateCard(
                            onRequestPermission = onRequestPermissions,
                            onOpenSingleFile = onOpenExternalFile
                        )
                        is LibraryUiState.Loading -> LoadingLibraryState()
                        is LibraryUiState.Empty -> EmptyLibraryState(
                            onRefresh = viewModel::refreshLibrary,
                            onOpenExternalFile = onOpenExternalFile
                        )
                        is LibraryUiState.Error -> EmptyLibraryState(
                            onRefresh = viewModel::refreshLibrary,
                            onOpenExternalFile = onOpenExternalFile
                        )
                        is LibraryUiState.Content -> {
                            when (selectedTab) {
                                0 -> MediaListTab(
                                    items = state.audioItems,
                                    currentPlayingId = playbackState.currentMedia?.id,
                                    onItemClick = { idx ->
                                        viewModel.playItems(state.audioItems, idx)
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                    onAddToPlaylist = { mediaItemForPlaylist = it },
                                    onRefresh = viewModel::refreshLibrary,
                                    onOpenExternalFile = onOpenExternalFile
                                )
                                1 -> MediaListTab(
                                    items = state.videoItems,
                                    currentPlayingId = playbackState.currentMedia?.id,
                                    onItemClick = { idx ->
                                        viewModel.playItems(state.videoItems, idx)
                                        isPlayerExpanded = true
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                    onAddToPlaylist = { mediaItemForPlaylist = it },
                                    onRefresh = viewModel::refreshLibrary,
                                    onOpenExternalFile = onOpenExternalFile
                                )
                                2 -> PlaylistsTab(
                                    playlists = playlists,
                                    allItems = state.audioItems + state.videoItems,
                                    favorites = state.favorites,
                                    onPlayQueue = { items ->
                                        viewModel.playItems(items, 0)
                                        if (items.firstOrNull()?.mediaType == MediaType.VIDEO) {
                                            isPlayerExpanded = true
                                        }
                                    },
                                    onDeletePlaylist = viewModel::deletePlaylist
                                )
                                3 -> FoldersTab(
                                    folders = state.folders,
                                    onPlayFolder = { items ->
                                        viewModel.playItems(items, 0)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    newPlaylistName = ""
                    showCreatePlaylistDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    mediaItemForPlaylist?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { mediaItemForPlaylist = null },
            title = { Text("Add to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created yet. Create a playlist in the Playlists tab first.")
                } else {
                    LazyColumn {
                        itemsIndexed(playlists) { _, pl ->
                            TextButton(
                                onClick = {
                                    viewModel.addTrackToPlaylist(pl.playlist.playlistId, targetItem.id)
                                    mediaItemForPlaylist = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${pl.playlist.name} (${pl.refs.size} tracks)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mediaItemForPlaylist = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MediaListTab(
    items: List<LocalMediaItem>,
    currentPlayingId: Long?,
    onItemClick: (Int) -> Unit,
    onToggleFavorite: (LocalMediaItem) -> Unit,
    onAddToPlaylist: (LocalMediaItem) -> Unit,
    onRefresh: () -> Unit,
    onOpenExternalFile: () -> Unit
) {
    if (items.isEmpty()) {
        EmptyLibraryState(onRefresh = onRefresh, onOpenExternalFile = onOpenExternalFile)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                MediaItemRow(
                    item = item,
                    isCurrentlyPlaying = item.id == currentPlayingId,
                    onClick = { onItemClick(index) },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onAddToPlaylist = { onAddToPlaylist(item) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<com.xplayer.media.data.local.PlaylistWithMediaIds>,
    allItems: List<LocalMediaItem>,
    favorites: List<LocalMediaItem>,
    onPlayQueue: (List<LocalMediaItem>) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    val itemMap = remember(allItems) { allItems.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                onClick = { if (favorites.isNotEmpty()) onPlayQueue(favorites) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Favorites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${favorites.size} items", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        itemsIndexed(playlists, key = { _, pl -> pl.playlist.playlistId }) { _, pl ->
            val resolvedItems = pl.refs.mapNotNull { itemMap[it.mediaId] }
            Card(
                onClick = { if (resolvedItems.isNotEmpty()) onPlayQueue(resolvedItems) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${resolvedItems.size} playable tracks", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDeletePlaylist(pl.playlist.playlistId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist")
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTab(
    folders: Map<String, List<LocalMediaItem>>,
    onPlayFolder: (List<LocalMediaItem>) -> Unit
) {
    val entries = folders.entries.toList()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.key }) { _, (folderName, items) ->
            Card(
                onClick = { onPlayFolder(items) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(folderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${items.size} media files", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabContent(
    userPrefs: com.xplayer.media.data.preferences.UserPrefs,
    onDarkThemeChanged: (Int) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onResumeVideoChanged: (Boolean) -> Unit,
    onAutoSkipCorruptedChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Appearance & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Material You Dynamic Colors", fontWeight = FontWeight.Medium)
                    Text("Adapt palette to wallpaper on Android 12+", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = userPrefs.dynamicColors, onCheckedChange = onDynamicColorChanged)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme Mode", fontWeight = FontWeight.Medium)
                Row {
                    listOf(0 to "Auto", 1 to "Light", 2 to "Dark").forEach { (mode, label) ->
                        TextButton(onClick = { onDarkThemeChanged(mode) }) {
                            Text(
                                text = label,
                                fontWeight = if (userPrefs.darkThemeMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        item {
            Text("Playback & Resilience", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Resume Video Position", fontWeight = FontWeight.Medium)
                    Text("Remember last timestamp for local videos", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = userPrefs.resumeVideoPlayback, onCheckedChange = onResumeVideoChanged)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Skip Corrupted Media", fontWeight = FontWeight.Medium)
                    Text("Skip broken or unsupported files automatically in queues", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = userPrefs.autoSkipCorrupted, onCheckedChange = onAutoSkipCorruptedChanged)
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onClearHistory) {
                Text("Clear Playback Resume History")
            }
        }
        item {
            Text(
                text = "100% Offline • Zero Network Permissions • No Ads or Tracking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
