package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import com.example.data.db.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MusicPlayerManager"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    // Playback state
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _equalizerSettings = MutableStateFlow(EqualizerSettings())
    val equalizerSettings: StateFlow<EqualizerSettings> = _equalizerSettings.asStateFlow()

    // Playlist / Queue
    private var originalQueue = listOf<SongEntity>()
    private var activeQueue = listOf<SongEntity>()
    private var currentQueueIndex = -1

    private var progressJob: Job? = null

    val sleepTimerManager = SleepTimerManager(scope) {
        pause()
    }

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition.toLong()
                            val dur = player.duration.toLong()
                            if (dur > 0) _durationMs.value = dur
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Progress tracker error: ${e.message}")
                    }
                }
                delay(250L)
            }
        }
    }

    fun playQueue(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        originalQueue = songs
        updateActiveQueue(startIndex)
        playSongAtIndex(startIndex)
    }

    fun playSong(song: SongEntity, currentList: List<SongEntity> = listOf(song)) {
        originalQueue = if (currentList.contains(song)) currentList else listOf(song) + currentList
        val index = originalQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        updateActiveQueue(index)
        playSongAtIndex(index)
    }

    private fun updateActiveQueue(preserveIndex: Int) {
        if (_isShuffle.value) {
            val current = originalQueue.getOrNull(preserveIndex)
            val shuffled = originalQueue.filter { it.id != current?.id }.shuffled()
            activeQueue = if (current != null) listOf(current) + shuffled else shuffled
            currentQueueIndex = 0
        } else {
            activeQueue = originalQueue
            currentQueueIndex = preserveIndex.coerceIn(0, (activeQueue.size - 1).coerceAtLeast(0))
        }
    }

    private fun playSongAtIndex(index: Int) {
        if (index < 0 || index >= activeQueue.size) return
        currentQueueIndex = index
        val song = activeQueue[index]
        _currentSong.value = song

        try {
            mediaPlayer?.release()
            releaseAudioEffects()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (song.mediaUri.startsWith("content://") || song.mediaUri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(song.mediaUri))
                } else {
                    setDataSource(song.mediaUri)
                }

                setOnPreparedListener { player ->
                    player.start()
                    _isPlaying.value = true
                    _durationMs.value = player.duration.toLong()
                    setupAudioEffects(player.audioSessionId)
                }

                setOnCompletionListener {
                    onTrackCompleted()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play song ${song.title}", e)
            _isPlaying.value = false
        }
    }

    private fun onTrackCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (currentQueueIndex < activeQueue.size - 1) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    seekTo(0)
                }
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying.value = false
                } else {
                    player.start()
                    _isPlaying.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Toggle play/pause failed", e)
            }
        } ?: run {
            if (activeQueue.isNotEmpty() && currentQueueIndex in activeQueue.indices) {
                playSongAtIndex(currentQueueIndex)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pause error", e)
        }
    }

    fun playNext() {
        if (activeQueue.isEmpty()) return
        val nextIndex = (currentQueueIndex + 1) % activeQueue.size
        playSongAtIndex(nextIndex)
    }

    fun playPrevious() {
        if (activeQueue.isEmpty()) return
        // If current playback > 3 seconds, replay current song
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }
        val prevIndex = if (currentQueueIndex - 1 < 0) activeQueue.size - 1 else currentQueueIndex - 1
        playSongAtIndex(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Seek error", e)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        val current = _currentSong.value
        if (newShuffle) {
            val shuffled = originalQueue.filter { it.id != current?.id }.shuffled()
            activeQueue = if (current != null) listOf(current) + shuffled else shuffled
            currentQueueIndex = 0
        } else {
            activeQueue = originalQueue
            currentQueueIndex = originalQueue.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    // Equalizer & AudioFX
    private fun setupAudioEffects(audioSessionId: Int) {
        try {
            if (equalizer == null) {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = _equalizerSettings.value.isEnabled

                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()

                val bandsList = (0 until numBands).map { bandIdx ->
                    val freq = eq.getCenterFreq(bandIdx.toShort()) / 1000 // mHz to Hz
                    val savedLevel = _equalizerSettings.value.bands.getOrNull(bandIdx)?.currentMillibels ?: 0
                    try {
                        eq.setBandLevel(bandIdx.toShort(), savedLevel.toShort())
                    } catch (_: Exception) {}

                    EqualizerBand(
                        index = bandIdx,
                        centerFreqHz = freq,
                        minMillibels = minLevel,
                        maxMillibels = maxLevel,
                        currentMillibels = savedLevel
                    )
                }

                val numPresets = eq.numberOfPresets.toInt()
                val presetNames = (0 until numPresets).map {
                    eq.getPresetName(it.toShort())
                }.ifEmpty { _equalizerSettings.value.presets }

                _equalizerSettings.value = _equalizerSettings.value.copy(
                    bands = bandsList.ifEmpty { _equalizerSettings.value.bands },
                    presets = presetNames
                )

                equalizer = eq
            }

            if (bassBoost == null) {
                val bb = BassBoost(0, audioSessionId)
                bb.enabled = _equalizerSettings.value.isEnabled
                if (bb.strengthSupported) {
                    bb.setStrength(_equalizerSettings.value.bassBoostStrength.toShort())
                }
                bassBoost = bb
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioFx Equalizer initialization not fully supported on this device/session", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerSettings.value = _equalizerSettings.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.d(TAG, "Equalizer toggle error: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val currentSettings = _equalizerSettings.value
        val updatedBands = currentSettings.bands.map { band ->
            if (band.index == bandIndex) band.copy(currentMillibels = levelMillibels) else band
        }
        _equalizerSettings.value = currentSettings.copy(
            bands = updatedBands,
            currentPresetIndex = -1 // custom
        )

        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMillibels.toShort())
        } catch (e: Exception) {
            Log.d(TAG, "Set band level error: ${e.message}")
        }
    }

    fun setPreset(presetIndex: Int) {
        val currentSettings = _equalizerSettings.value
        if (presetIndex in currentSettings.presets.indices) {
            try {
                equalizer?.usePreset(presetIndex.toShort())
                val numBands = equalizer?.numberOfBands?.toInt() ?: currentSettings.bands.size
                val updatedBands = currentSettings.bands.mapIndexed { idx, band ->
                    val level = if (idx < numBands) equalizer?.getBandLevel(idx.toShort())?.toInt() ?: 0 else 0
                    band.copy(currentMillibels = level)
                }
                _equalizerSettings.value = currentSettings.copy(
                    currentPresetIndex = presetIndex,
                    bands = updatedBands
                )
            } catch (e: Exception) {
                Log.d(TAG, "Preset error: ${e.message}")
                // Fallback simulation presets
                val simulatedBands = getSimulatedPresetBands(presetIndex, currentSettings.bands)
                _equalizerSettings.value = currentSettings.copy(
                    currentPresetIndex = presetIndex,
                    bands = simulatedBands
                )
            }
        }
    }

    private fun getSimulatedPresetBands(presetIndex: Int, currentBands: List<EqualizerBand>): List<EqualizerBand> {
        val deltas = when (presetIndex) {
            0 -> listOf(0, 0, 0, 0, 0) // Normal
            1 -> listOf(400, 300, -100, 200, 300) // Classical
            2 -> listOf(600, 400, 100, 400, 500) // Dance
            3 -> listOf(0, 0, 0, 0, 0) // Flat
            4 -> listOf(300, 100, 0, 200, -100) // Folk
            5 -> listOf(500, 200, -100, 300, 600) // Heavy Metal
            6 -> listOf(700, 500, 0, 200, 400) // Hip Hop
            7 -> listOf(300, 200, 100, 200, 400) // Jazz
            8 -> listOf(-100, 200, 400, 200, -100) // Pop
            9 -> listOf(600, 400, -200, 300, 500) // Rock
            else -> listOf(800, 600, 0, 0, 200) // Bass Boost
        }
        return currentBands.mapIndexed { idx, band ->
            val delta = deltas.getOrElse(idx) { 0 }
            band.copy(currentMillibels = delta)
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerSettings.value = _equalizerSettings.value.copy(bassBoostStrength = clamped)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.d(TAG, "Bass boost set error: ${e.message}")
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
        } catch (e: Exception) {
            Log.d(TAG, "Audio effects release error: ${e.message}")
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerManager.cancelTimer()
        releaseAudioEffects()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
