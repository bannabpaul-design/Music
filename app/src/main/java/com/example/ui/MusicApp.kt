package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.CustomCoverDialog
import com.example.ui.components.EditSongDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SongsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(viewModel: MainViewModel) {
    val songs by viewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val equalizerSettings by viewModel.equalizerSettings.collectAsStateWithLifecycle()
    val remainingSleepSeconds by viewModel.remainingSleepSeconds.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val activePlaylistDetail by viewModel.activePlaylistDetail.collectAsStateWithLifecycle()
    val activePlaylistSongs by viewModel.activePlaylistSongs.collectAsStateWithLifecycle()

    val editingSong by viewModel.editingSong.collectAsStateWithLifecycle()
    val customCoverSong by viewModel.customCoverSong.collectAsStateWithLifecycle()
    val addToPlaylistSong by viewModel.addToPlaylistSong.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isNowPlayingExpanded && activePlaylistDetail == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PrimaryNeon,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Melody (ចាក់តន្ត្រី)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openSleepTimerDialog() },
                            modifier = Modifier.testTag("appbar_sleep_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (remainingSleepSeconds != null) SecondaryCyan else Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            }
        },
        bottomBar = {
            if (!isNowPlayingExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Player
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onExpand = { viewModel.setNowPlayingExpanded(true) }
                        )
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.SONGS && activePlaylistDetail == null,
                            onClick = { viewModel.selectTab(NavigationTab.SONGS) },
                            icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                            label = { Text("បទចម្រៀង") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryNeon,
                                indicatorColor = PrimaryNeon
                            ),
                            modifier = Modifier.testTag("nav_tab_songs")
                        )
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.PLAYLISTS || activePlaylistDetail != null,
                            onClick = { viewModel.selectTab(NavigationTab.PLAYLISTS) },
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                            label = { Text("បញ្ជីចម្រៀង") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryNeon,
                                indicatorColor = PrimaryNeon
                            ),
                            modifier = Modifier.testTag("nav_tab_playlists")
                        )
                        NavigationBarItem(
                            selected = currentTab == NavigationTab.EQUALIZER,
                            onClick = { viewModel.selectTab(NavigationTab.EQUALIZER) },
                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                            label = { Text("Equalizer") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = PrimaryNeon,
                                indicatorColor = PrimaryNeon
                            ),
                            modifier = Modifier.testTag("nav_tab_equalizer")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content depending on active selection
            when {
                activePlaylistDetail != null -> {
                    PlaylistDetailScreen(
                        playlist = activePlaylistDetail!!,
                        songs = activePlaylistSongs,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        onBack = { viewModel.closePlaylistDetail() },
                        onPlayAll = {
                            if (activePlaylistSongs.isNotEmpty()) {
                                viewModel.playSong(activePlaylistSongs[0], activePlaylistSongs)
                            }
                        },
                        onPlaySong = { song -> viewModel.playSong(song, activePlaylistSongs) },
                        onAddSongs = {
                            // Prompt to add songs
                            if (songs.isNotEmpty()) {
                                viewModel.openAddToPlaylistDialog(songs[0])
                            }
                        },
                        onRemoveSongFromPlaylist = { songId ->
                            viewModel.removeSongFromPlaylist(activePlaylistDetail!!.id, songId)
                        },
                        onEditSongTags = { song -> viewModel.openEditSongDialog(song) },
                        onChangeSongCover = { song -> viewModel.openCustomCoverDialog(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) }
                    )
                }
                currentTab == NavigationTab.SONGS -> {
                    SongsScreen(
                        songs = songs,
                        currentSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        onSongClick = { song -> viewModel.playSong(song, songs) },
                        onEditSongTags = { song -> viewModel.openEditSongDialog(song) },
                        onChangeSongCover = { song -> viewModel.openCustomCoverDialog(song) },
                        onAddToPlaylist = { song -> viewModel.openAddToPlaylistDialog(song) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onDeleteSong = { song -> viewModel.deleteSong(song) },
                        onImportUris = { uris -> viewModel.importAudioUris(uris) },
                        onScanDevice = { viewModel.scanDeviceAudio() }
                    )
                }
                currentTab == NavigationTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = playlists,
                        onSelectPlaylist = { playlist -> viewModel.openPlaylistDetail(playlist) },
                        onCreatePlaylist = { viewModel.openCreatePlaylistDialog() },
                        onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist) }
                    )
                }
                currentTab == NavigationTab.EQUALIZER -> {
                    EqualizerScreen(
                        equalizerSettings = equalizerSettings,
                        onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                        onSelectPreset = { viewModel.setEqualizerPreset(it) },
                        onBandLevelChange = { bandIdx, level -> viewModel.setBandLevel(bandIdx, level) },
                        onBassBoostChange = { viewModel.setBassBoost(it) }
                    )
                }
            }
        }
    }

    // Now Playing Fullscreen Overlay
    AnimatedVisibility(
        visible = isNowPlayingExpanded && currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        currentSong?.let { song ->
            NowPlayingScreen(
                song = song,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                remainingSleepSeconds = remainingSleepSeconds,
                onCollapse = { viewModel.setNowPlayingExpanded(false) },
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleFavorite = { viewModel.toggleFavorite(song) },
                onOpenEqualizer = {
                    viewModel.setNowPlayingExpanded(false)
                    viewModel.selectTab(NavigationTab.EQUALIZER)
                },
                onOpenSleepTimer = { viewModel.openSleepTimerDialog() },
                onEditSong = { viewModel.openEditSongDialog(song) },
                onChangeCover = { viewModel.openCustomCoverDialog(song) }
            )
        }
    }

    // Dialogs
    editingSong?.let { song ->
        EditSongDialog(
            song = song,
            onDismiss = { viewModel.closeEditSongDialog() },
            onSave = { title, artist, album ->
                viewModel.saveSongMetadata(song.id, title, artist, album)
            }
        )
    }

    customCoverSong?.let { song ->
        CustomCoverDialog(
            song = song,
            onDismiss = { viewModel.closeCustomCoverDialog() },
            onSaveCover = { coverUri ->
                viewModel.saveCustomCover(song.id, coverUri)
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.closeCreatePlaylistDialog() },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
            }
        )
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = playlists,
            onDismiss = { viewModel.closeAddToPlaylistDialog() },
            onSelectPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
            },
            onCreateNewPlaylist = {
                viewModel.closeAddToPlaylistDialog()
                viewModel.openCreatePlaylistDialog()
            }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingSeconds = remainingSleepSeconds,
            onDismiss = { viewModel.closeSleepTimerDialog() },
            onSetTimer = { minutes -> viewModel.setSleepTimer(minutes) },
            onCancelTimer = { viewModel.cancelSleepTimer() }
        )
    }
}
