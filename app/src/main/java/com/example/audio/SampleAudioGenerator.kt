package com.example.audio

import android.content.Context
import android.util.Log
import com.example.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SampleAudioGenerator {
    private const val TAG = "SampleAudioGenerator"

    suspend fun generateSampleTracks(context: Context): List<SongEntity> = withContext(Dispatchers.IO) {
        val samplesDir = File(context.filesDir, "samples").apply { if (!exists()) mkdirs() }
        val generatedSongs = mutableListOf<SongEntity>()

        val trackSpecs = listOf(
            SampleSpec(
                fileName = "chaktomuk_breeze.wav",
                title = "រាត្រីចតុមុខ (Chaktomuk Breeze)",
                artist = "Melody Heritage",
                album = "Khmer Melodies Vol. 1",
                coverUri = "preset:neon_cyan",
                durationSeconds = 24,
                generatorType = GeneratorType.PENTATONIC_ACOUSTIC
            ),
            SampleSpec(
                fileName = "sunset_acoustic.wav",
                title = "ពន្លឺថ្ងៃរៀបលិច (Golden Sunset)",
                artist = "Acoustic Vibe",
                album = "Warm Acoustic Dreams",
                coverUri = "drawable:cover_acoustic",
                durationSeconds = 22,
                generatorType = GeneratorType.WARM_CHORDS
            ),
            SampleSpec(
                fileName = "cyber_neon.wav",
                title = "Cyber Neon Bass",
                artist = "Synthwave Pulse",
                album = "Electric Night",
                coverUri = "preset:magenta_violet",
                durationSeconds = 20,
                generatorType = GeneratorType.SYNTH_BASS
            ),
            SampleSpec(
                fileName = "lofi_midnight.wav",
                title = "Midnight Lo-Fi Dream",
                artist = "Chill Horizon",
                album = "Night Coffee Sessions",
                coverUri = "drawable:cover_lofi",
                durationSeconds = 25,
                generatorType = GeneratorType.LOFI_PIANO
            )
        )

        for (spec in trackSpecs) {
            val audioFile = File(samplesDir, spec.fileName)
            if (!audioFile.exists() || audioFile.length() < 1000) {
                try {
                    createWavFile(audioFile, spec.durationSeconds, spec.generatorType)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate sample: ${spec.fileName}", e)
                }
            }

            if (audioFile.exists() && audioFile.length() > 0) {
                generatedSongs.add(
                    SongEntity(
                        id = "sample_${spec.fileName}",
                        title = spec.title,
                        artist = spec.artist,
                        album = spec.album,
                        mediaUri = audioFile.absolutePath,
                        durationMs = spec.durationSeconds * 1000L,
                        customCoverUri = spec.coverUri,
                        isSample = true
                    )
                )
            }
        }

        generatedSongs
    }

    private fun createWavFile(file: File, durationSeconds: Int, type: GeneratorType) {
        val sampleRate = 22050 // light weight and fast to generate
        val totalSamples = sampleRate * durationSeconds
        val numChannels = 2 // Stereo
        val bytesPerSample = 2 // 16-bit
        val dataSize = totalSamples * numChannels * bytesPerSample

        FileOutputStream(file).use { fos ->
            // Write 44-byte WAV header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1) // AudioFormat 1 = PCM
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            header.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            header.putShort(16) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)

            fos.write(header.array())

            // Write PCM audio buffer in chunks
            val chunkSize = 2048
            val buffer = ByteArray(chunkSize * numChannels * bytesPerSample)
            val byteBuf = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

            var sampleIdx = 0
            while (sampleIdx < totalSamples) {
                val samplesToWrite = minOf(chunkSize, totalSamples - sampleIdx)
                byteBuf.clear()

                for (i in 0 until samplesToWrite) {
                    val t = (sampleIdx + i).toDouble() / sampleRate
                    val (left, right) = generateStereoSample(t, type)
                    val leftShort = (left.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    val rightShort = (right.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    byteBuf.putShort(leftShort)
                    byteBuf.putShort(rightShort)
                }

                fos.write(buffer, 0, samplesToWrite * numChannels * bytesPerSample)
                sampleIdx += samplesToWrite
            }
        }
    }

    private fun generateStereoSample(t: Double, type: GeneratorType): Pair<Double, Double> {
        return when (type) {
            GeneratorType.PENTATONIC_ACOUSTIC -> {
                // Gentle pentatonic chime progression (C4, D4, E4, G4, A4, C5)
                val notes = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 392.00, 329.63)
                val beat = (t * 2.0).toInt() % notes.size
                val noteT = (t * 2.0) - (t * 2.0).toInt()
                val freq = notes[beat]
                val envelope = exp(-noteT * 3.5)
                val wave = (sin(2 * PI * freq * t) * 0.6 + sin(2 * PI * freq * 2 * t) * 0.25) * envelope

                // Soft pad underneath
                val pad = sin(2 * PI * 130.81 * t) * 0.15 + sin(2 * PI * 196.00 * t) * 0.1
                val left = (wave * 0.7 + pad) * 0.5
                val right = (wave * 0.8 + pad) * 0.5
                Pair(left, right)
            }
            GeneratorType.WARM_CHORDS -> {
                // Arpeggiated warm guitar chords
                val chordIndex = (t / 2.0).toInt() % 4
                val chordFreqs = when (chordIndex) {
                    0 -> doubleArrayOf(220.0, 261.63, 329.63, 440.0) // Am
                    1 -> doubleArrayOf(174.61, 220.0, 261.63, 349.23) // F
                    2 -> doubleArrayOf(261.63, 329.63, 392.0, 523.25) // C
                    else -> doubleArrayOf(196.0, 246.94, 293.66, 392.0) // G
                }
                val subBeat = ((t * 4.0).toInt()) % 4
                val freq = chordFreqs[subBeat]
                val subT = (t * 4.0) - (t * 4.0).toInt()
                val env = exp(-subT * 4.0)
                val tone = sin(2 * PI * freq * t) * env * 0.45
                Pair(tone * 0.8, tone * 0.65)
            }
            GeneratorType.SYNTH_BASS -> {
                // Synthwave bass + electronic kick rhythm
                val bassFreq = when (((t * 1.5).toInt()) % 4) {
                    0 -> 55.0  // A1
                    1 -> 65.41 // C2
                    2 -> 49.0  // G1
                    else -> 43.65 // F1
                }
                val kickT = (t * 2.0) - (t * 2.0).toInt()
                val kick = sin(2 * PI * (120.0 * exp(-kickT * 18.0)) * t) * exp(-kickT * 8.0) * 0.6
                val bass = (sin(2 * PI * bassFreq * t) + 0.5 * sin(2 * PI * bassFreq * 2 * t)) * 0.35
                val lead = sin(2 * PI * 440.0 * t) * 0.08
                Pair((kick + bass + lead) * 0.6, (kick + bass * 0.9 + lead) * 0.6)
            }
            GeneratorType.LOFI_PIANO -> {
                // Mellow lo-fi Rhodes chords with soft vinyl hiss
                val chordTime = (t / 3.0).toInt() % 4
                val root = when (chordTime) {
                    0 -> 174.61 // Fmaj7
                    1 -> 164.81 // E7
                    2 -> 220.00 // Am7
                    else -> 196.00 // G7
                }
                val chordT = (t / 3.0) - (t / 3.0).toInt()
                val env = exp(-chordT * 1.5)
                val piano = (sin(2 * PI * root * t) * 0.4 +
                        sin(2 * PI * (root * 1.2599) * t) * 0.25 +
                        sin(2 * PI * (root * 1.4983) * t) * 0.25) * env
                // subtle vinyl texture
                val texture = (sin(2 * PI * 3400.0 * t) * 0.02)
                Pair((piano + texture) * 0.5, (piano * 0.95 + texture) * 0.5)
            }
        }
    }

    private data class SampleSpec(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val coverUri: String,
        val durationSeconds: Int,
        val generatorType: GeneratorType
    )

    private enum class GeneratorType {
        PENTATONIC_ACOUSTIC,
        WARM_CHORDS,
        SYNTH_BASS,
        LOFI_PIANO
    }
}
