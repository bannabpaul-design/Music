package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.SongEntity
import com.example.ui.components.SongListItem
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan

enum class SongFilter {
    ALL,
    FAVORITES,
    SAMPLES,
    IMPORTED
}

@Composable
fun SongsScreen(
    songs: List<SongEntity>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (SongEntity) -> Unit,
    onEditSongTags: (SongEntity) -> Unit,
    onChangeSongCover: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onToggleFavorite: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onScanDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(SongFilter.ALL) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            onImportUris(uris)
        }
    }

    val filteredSongs = remember(songs, searchQuery, currentFilter) {
        songs.filter { song ->
            val matchesSearch = searchQuery.isBlank() ||
                    song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true) ||
                    song.album.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (currentFilter) {
                SongFilter.ALL -> true
                SongFilter.FAVORITES -> song.isFavorite
                SongFilter.SAMPLES -> song.isSample
                SongFilter.IMPORTED -> !song.isSample
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Import Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ស្វែងរកបទចម្រៀង ឬអ្នកនិពន្ធ... (Search)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryNeon,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = DarkSurfaceHighlight,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("songs_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Buttons (Import Audio Files, Scan Device)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("audio/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "នាំចូល (Import)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onScanDevice,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = PrimaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ស្កេន (Scan)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == SongFilter.ALL,
                        onClick = { currentFilter = SongFilter.ALL },
                        label = { Text("ទាំងអស់ (${songs.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val favCount = songs.count { it.isFavorite }
                    FilterChip(
                        selected = currentFilter == SongFilter.FAVORITES,
                        onClick = { currentFilter = SongFilter.FAVORITES },
                        label = { Text("សំណព្វ ($favCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val sampleCount = songs.count { it.isSample }
                    FilterChip(
                        selected = currentFilter == SongFilter.SAMPLES,
                        onClick = { currentFilter = SongFilter.SAMPLES },
                        label = { Text("បទគំរូ ($sampleCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val importedCount = songs.count { !it.isSample }
                    FilterChip(
                        selected = currentFilter == SongFilter.IMPORTED,
                        onClick = { currentFilter = SongFilter.IMPORTED },
                        label = { Text("បាននាំចូល ($importedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Song List
        if (filteredSongs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "រកមិនឃើញបទចម្រៀងទេ (No songs found)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ចុច 'នាំចូល' ឬ 'ស្កេន' ដើម្បីបន្ថែមបទចម្រៀង MP3, AAC, WAV",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentSongId
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && isCurrent,
                        isCurrent = isCurrent,
                        onClick = { onSongClick(song) },
                        onEditTags = { onEditSongTags(song) },
                        onChangeCover = { onChangeSongCover(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onDeleteSong = { onDeleteSong(song) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
