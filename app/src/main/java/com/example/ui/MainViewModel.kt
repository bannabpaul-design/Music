package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.MusicDatabase
import com.example.data.db.PlaylistEntity
import com.example.data.db.SongEntity
import com.example.data.repository.MusicRepository
import com.example.player.EqualizerSettings
import com.example.player.MusicPlayerManager
import com.example.player.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
    SONGS,
    PLAYLISTS,
    EQUALIZER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao(), application)
    val playerManager = MusicPlayerManager(application, viewModelScope)

    // Data streams
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player state delegated from playerManager
    val currentSong: StateFlow<SongEntity?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val isShuffle: StateFlow<Boolean> = playerManager.isShuffle
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val equalizerSettings: StateFlow<EqualizerSettings> = playerManager.equalizerSettings
    val remainingSleepSeconds: StateFlow<Int?> = playerManager.sleepTimerManager.remainingSeconds

    // UI State
    private val _currentTab = MutableStateFlow(NavigationTab.SONGS)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _activePlaylistDetail = MutableStateFlow<PlaylistEntity?>(null)
    val activePlaylistDetail: StateFlow<PlaylistEntity?> = _activePlaylistDetail.asStateFlow()

    val activePlaylistSongs: StateFlow<List<SongEntity>> = _activePlaylistDetail
        .flatMapLatest { playlist ->
            if (playlist != null) repository.getSongsForPlaylist(playlist.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dialogs state
    private val _editingSong = MutableStateFlow<SongEntity?>(null)
    val editingSong: StateFlow<SongEntity?> = _editingSong.asStateFlow()

    private val _customCoverSong = MutableStateFlow<SongEntity?>(null)
    val customCoverSong: StateFlow<SongEntity?> = _customCoverSong.asStateFlow()

    private val _addToPlaylistSong = MutableStateFlow<SongEntity?>(null)
    val addToPlaylistSong: StateFlow<SongEntity?> = _addToPlaylistSong.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSamplesIfNeeded()
        }
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
        if (_activePlaylistDetail.value != null && tab != NavigationTab.PLAYLISTS) {
            _activePlaylistDetail.value = null
        }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun openPlaylistDetail(playlist: PlaylistEntity) {
        _activePlaylistDetail.value = playlist
    }

    fun closePlaylistDetail() {
        _activePlaylistDetail.value = null
    }

    // Playback actions
    fun playSong(song: SongEntity, list: List<SongEntity>? = null) {
        val queue = list ?: allSongs.value
        playerManager.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    // Equalizer
    fun setEqualizerEnabled(enabled: Boolean) {
        playerManager.setEqualizerEnabled(enabled)
    }

    fun setEqualizerPreset(presetIndex: Int) {
        playerManager.setPreset(presetIndex)
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        playerManager.setBandLevel(bandIndex, levelMillibels)
    }

    fun setBassBoost(strength: Int) {
        playerManager.setBassBoost(strength)
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        playerManager.sleepTimerManager.startTimer(minutes)
        _snackbarMessage.value = "បានកំណត់ម៉ោងបិទស្វ័យប្រវត្តិចំនួន $minutes នាទី"
    }

    fun cancelSleepTimer() {
        playerManager.sleepTimerManager.cancelTimer()
        _snackbarMessage.value = "បានបិទកម្មវិធីកំណត់ម៉ោង"
    }

    // Metadata & Covers
    fun openEditSongDialog(song: SongEntity) {
        _editingSong.value = song
    }

    fun closeEditSongDialog() {
        _editingSong.value = null
    }

    fun saveSongMetadata(songId: String, title: String, artist: String, album: String) {
        viewModelScope.launch {
            repository.updateSongMetadata(songId, title, artist, album)
            closeEditSongDialog()
            _snackbarMessage.value = "បានរក្សាទុកព័ត៌មានបទចម្រៀង"
        }
    }

    fun openCustomCoverDialog(song: SongEntity) {
        _customCoverSong.value = song
    }

    fun closeCustomCoverDialog() {
        _customCoverSong.value = null
    }

    fun saveCustomCover(songId: String, coverUri: String?) {
        viewModelScope.launch {
            repository.updateSongCover(songId, coverUri)
            closeCustomCoverDialog()
            _snackbarMessage.value = "បានប្ដូររូបគម្របអាល់ប៊ុមដោយជោគជ័យ"
        }
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            repository.deleteSong(song)
            _snackbarMessage.value = "បានលុបបទចម្រៀង"
        }
    }

    // Playlists
    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            closeCreatePlaylistDialog()
            _snackbarMessage.value = "បានបង្កើតបញ្ជីចម្រៀង \"$name\""
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (_activePlaylistDetail.value?.id == playlist.id) {
                _activePlaylistDetail.value = null
            }
            _snackbarMessage.value = "បានលុបបញ្ជីចម្រៀង \"${playlist.name}\""
        }
    }

    fun openAddToPlaylistDialog(song: SongEntity) {
        _addToPlaylistSong.value = song
    }

    fun closeAddToPlaylistDialog() {
        _addToPlaylistSong.value = null
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            closeAddToPlaylistDialog()
            _snackbarMessage.value = "បានបញ្ចូលបទចម្រៀងទៅកាន់បញ្ជី"
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _snackbarMessage.value = "បានដកចេញពីបញ្ជីចម្រៀង"
        }
    }

    // Dialog triggers
    fun openSleepTimerDialog() {
        _showSleepTimerDialog.value = true
    }

    fun closeSleepTimerDialog() {
        _showSleepTimerDialog.value = false
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Device audio scan & import
    fun scanDeviceAudio() {
        viewModelScope.launch {
            val count = repository.scanDeviceAudio()
            _snackbarMessage.value = if (count > 0) "បានស្កេនឃើញបទចម្រៀង $count បទ" else "មិនមានឯកសារចម្រៀងថ្មីនៅក្នុងឧបករណ៍ទេ"
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        viewModelScope.launch {
            val count = repository.importAudioUris(uris)
            _snackbarMessage.value = "បាននាំចូលបទចម្រៀង $count បទដោយជោគជ័យ"
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
