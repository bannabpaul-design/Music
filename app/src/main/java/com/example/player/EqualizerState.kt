package com.example.player

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val minMillibels: Int = -1500,
    val maxMillibels: Int = 1500,
    val currentMillibels: Int = 0
) {
    val displayFrequency: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }

    val displayDb: String
        get() {
            val db = currentMillibels / 100
            return if (db > 0) "+$db dB" else "$db dB"
        }
}

data class EqualizerSettings(
    val isEnabled: Boolean = true,
    val currentPresetIndex: Int = 0,
    val presets: List<String> = listOf("Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", "Hip Hop", "Jazz", "Pop", "Rock", "Bass Boost"),
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, -1500, 1500, 0),
        EqualizerBand(1, 230, -1500, 1500, 0),
        EqualizerBand(2, 910, -1500, 1500, 0),
        EqualizerBand(3, 3600, -1500, 1500, 0),
        EqualizerBand(4, 14000, -1500, 1500, 0)
    ),
    val bassBoostStrength: Int = 300 // 0..1000
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
