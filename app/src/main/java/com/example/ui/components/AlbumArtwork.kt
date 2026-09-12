package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import kotlin.math.abs

@Composable
fun AlbumArtwork(
    coverUri: String?,
    songTitle: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 28.dp
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            coverUri == "drawable:cover_lofi" -> {
                AsyncImage(
                    model = R.drawable.cover_lofi,
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverUri == "drawable:cover_acoustic" -> {
                AsyncImage(
                    model = R.drawable.cover_acoustic,
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            coverUri?.startsWith("preset:") == true -> {
                val presetName = coverUri.removePrefix("preset:")
                val brush = getPresetBrush(presetName)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
            !coverUri.isNullOrBlank() -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(coverUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Custom Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        FallbackGradientArt(songTitle, iconSize)
                    }
                )
            }
            else -> {
                FallbackGradientArt(songTitle, iconSize)
            }
        }
    }
}

@Composable
private fun FallbackGradientArt(seed: String, iconSize: Dp) {
    val brush = generateGradientFromSeed(seed)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize)
        )
    }
}

fun getPresetBrush(presetName: String): Brush {
    return when (presetName) {
        "neon_cyan" -> Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
        "magenta_violet" -> Brush.linearGradient(listOf(Color(0xFFF72585), Color(0xFF7209B7)))
        "golden_amber" -> Brush.linearGradient(listOf(Color(0xFFF7971E), Color(0xFFFFD200)))
        "emerald_chill" -> Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
        "electric_purple" -> Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
        "cyber_blue" -> Brush.linearGradient(listOf(Color(0xFF4E54C8), Color(0xFF8F94FB)))
        else -> Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)))
    }
}

private fun generateGradientFromSeed(seed: String): Brush {
    val hash = abs(seed.hashCode())
    val palettes = listOf(
        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)),
        listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)),
        listOf(Color(0xFFCC2B5E), Color(0xFF753A88)),
        listOf(Color(0xFF42275A), Color(0xFF734B6D)),
        listOf(Color(0xFF141E30), Color(0xFF243B55)),
        listOf(Color(0xFF0575E6), Color(0xFF00F260)),
        listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
    )
    val chosen = palettes[hash % palettes.size]
    return Brush.linearGradient(chosen)
}
