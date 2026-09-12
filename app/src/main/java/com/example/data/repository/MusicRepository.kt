package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.example.audio.SampleAudioGenerator
import com.example.data.db.MusicDao
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongCrossRef
import com.example.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(
    private val musicDao: MusicDao,
    private val context: Context
) {
    val allSongs: Flow<List<SongEntity>> = musicDao.getAllSongs()
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return musicDao.getSongsForPlaylist(playlistId)
    }

    suspend fun initializeSamplesIfNeeded() = withContext(Dispatchers.IO) {
        val samples = SampleAudioGenerator.generateSampleTracks(context)
        musicDao.insertSongs(samples)

        // If no playlists exist, create a default "Favorites" or "Chill Vibes" playlist
        val defaultPlaylists = listOf(
            PlaylistEntity(name = "បទចម្រៀងសំណព្វ (Favorites)", description = "My favorite tracks"),
            PlaylistEntity(name = "សម្រាកអារម្មណ៍ (Chill Vibes)", description = "Relaxing & acoustic music")
        )
        for (pl in defaultPlaylists) {
            val id = musicDao.insertPlaylist(pl)
            if (id > 0 && samples.isNotEmpty()) {
                musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = id, songId = samples[0].id))
            }
        }
    }

    suspend fun scanDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = mutableListOf<SongEntity>()
        var count = 0
        try {
            val cursor: Cursor? = resolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durCol)
                    val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()

                    songs.add(
                        SongEntity(
                            id = "device_$id",
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            mediaUri = contentUri,
                            durationMs = duration,
                            isSample = false
                        )
                    )
                    count++
                }
            }
            if (songs.isNotEmpty()) {
                musicDao.insertSongs(songs)
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning device audio", e)
        }
        count
    }

    suspend fun importAudioUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val songs = mutableListOf<SongEntity>()
        val retriever = MediaMetadataRetriever()

        for (uri in uris) {
            try {
                // Try reading display name from OpenableColumns
                var displayName = "Audio File"
                var duration = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }

                var title = displayName.substringBeforeLast(".")
                var artist = "Unknown Artist"
                var album = "Imported Music"

                try {
                    retriever.setDataSource(context, uri)
                    val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val metaAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                    if (!metaTitle.isNullOrBlank()) title = metaTitle
                    if (!metaArtist.isNullOrBlank()) artist = metaArtist
                    if (!metaAlbum.isNullOrBlank()) album = metaAlbum
                    if (!metaDuration.isNullOrBlank()) duration = metaDuration.toLongOrNull() ?: 0L
                } catch (e: Exception) {
                    Log.w("MusicRepository", "Could not extract metadata for $uri", e)
                }

                val songId = "imported_${uri.hashCode()}_${System.currentTimeMillis()}"
                songs.add(
                    SongEntity(
                        id = songId,
                        title = title,
                        artist = artist,
                        album = album,
                        mediaUri = uri.toString(),
                        durationMs = duration,
                        isSample = false
                    )
                )
                importedCount++
            } catch (e: Exception) {
                Log.e("MusicRepository", "Failed to import uri: $uri", e)
            }
        }

        try {
            retriever.release()
        } catch (_: Exception) {}

        if (songs.isNotEmpty()) {
            musicDao.insertSongs(songs)
        }
        importedCount
    }

    suspend fun updateSongMetadata(songId: String, title: String, artist: String, album: String) {
        withContext(Dispatchers.IO) {
            musicDao.updateSongMetadata(songId, title, artist, album)
        }
    }

    suspend fun updateSongCover(songId: String, coverUri: String?) {
        withContext(Dispatchers.IO) {
            musicDao.updateSongCover(songId, coverUri)
        }
    }

    suspend fun toggleFavorite(song: SongEntity) {
        withContext(Dispatchers.IO) {
            musicDao.updateFavorite(song.id, !song.isFavorite)
        }
    }

    suspend fun deleteSong(song: SongEntity) {
        withContext(Dispatchers.IO) {
            musicDao.deleteSong(song)
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return withContext(Dispatchers.IO) {
            musicDao.insertPlaylist(
                PlaylistEntity(
                    name = name,
                    description = description
                )
            )
        }
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity) {
        withContext(Dispatchers.IO) {
            musicDao.updatePlaylist(playlist)
        }
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        withContext(Dispatchers.IO) {
            musicDao.deletePlaylistSongs(playlist.id)
            musicDao.deletePlaylist(playlist)
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        withContext(Dispatchers.IO) {
            musicDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        withContext(Dispatchers.IO) {
            musicDao.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
