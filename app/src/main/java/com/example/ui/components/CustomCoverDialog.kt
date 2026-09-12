package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.data.db.SongEntity
import com.example.ui.theme.PrimaryNeon

@Composable
fun CustomCoverDialog(
    song: SongEntity,
    onDismiss: () -> Unit,
    onSaveCover: (coverUri: String?) -> Unit
) {
    var selectedCoverUri by remember { mutableStateOf(song.customCoverUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedCoverUri = it.toString()
        }
    }

    val presetOptions = listOf(
        "preset:neon_cyan" to "Neon Cyan",
        "preset:magenta_violet" to "Magenta",
        "preset:golden_amber" to "Golden Sun",
        "preset:emerald_chill" to "Emerald",
        "preset:electric_purple" to "Electric",
        "preset:cyber_blue" to "Cyber Blue",
        "drawable:cover_lofi" to "Lo-Fi Art",
        "drawable:cover_acoustic" to "Acoustic Art"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "កំណត់រូបភាពគម្រប (Custom Album Cover)")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live preview
                AlbumArtwork(
                    coverUri = selectedCoverUri,
                    songTitle = song.title,
                    modifier = Modifier.size(130.dp),
                    cornerRadius = 16.dp,
                    iconSize = 48.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("ជ្រើសរើសរូបពី Gallery (Pick Photo)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ឬជ្រើសរើសស្ទាយសិល្បៈ (Preset Art Styles):",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetOptions) { (key, name) ->
                        val isSelected = selectedCoverUri == key
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) PrimaryNeon else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCoverUri = key },
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArtwork(
                                coverUri = key,
                                songTitle = name,
                                modifier = Modifier.size(50.dp),
                                cornerRadius = 10.dp,
                                iconSize = 16.dp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(PrimaryNeon, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedCoverUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { selectedCoverUri = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remove_cover_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("កំណត់ដូចដើម (Reset Default)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCover(selectedCoverUri)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                modifier = Modifier.testTag("save_cover_button")
            ) {
                Text("យល់ព្រម (Apply)")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_cover_button")
            ) {
                Text("បោះបង់ (Cancel)")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
