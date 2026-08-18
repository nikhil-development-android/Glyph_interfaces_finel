package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural Audio Synthesizer for Nothing Glyph Soundboard and Ringtones.
 * Generates authentic 8-bit / glitch / minimalist electronic tones on the fly.
 */
class GlyphSoundSynth(private val scope: CoroutineScope) {

    private val sampleRate = 44100

    fun playTone(toneType: SoundType) {
        scope.launch(Dispatchers.Default) {
            try {
                val samples = when (toneType) {
                    SoundType.CLICK -> generateClick()
                    SoundType.BLEEP_HIGH -> generateSine(880.0, 0.09, decay = 25.0)
                    SoundType.BLEEP_MID -> generateSine(440.0, 0.12, decay = 18.0)
                    SoundType.BLEEP_LOW -> generateSine(220.0, 0.18, decay = 12.0)
                    SoundType.KICK -> generateKick()
                    SoundType.GLITCH -> generateGlitch()
                    SoundType.SWEEP -> generateSweep()
                    SoundType.NOTHING_CHIME -> generateChime()
                }
                playPcm(samples)
            } catch (e: Exception) {
                // Fallback silently if audio output unavailable
            }
        }
    }

    fun playFrequency(freq: Double, durationSec: Double = 0.1, decay: Double = 15.0) {
        scope.launch(Dispatchers.Default) {
            try {
                val samples = generateSine(freq, durationSec, decay)
                playPcm(samples)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun playPcm(buffer: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
    }

    private fun generateSine(freq: Double, durationSec: Double, decay: Double = 10.0): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-decay * t)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.7).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateClick(): ShortArray {
        val numSamples = (sampleRate * 0.03).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-80.0 * t)
            val sample = (sin(2.0 * PI * 1800.0 * t) * env * Short.MAX_VALUE * 0.9).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateKick(): ShortArray {
        val numSamples = (sampleRate * 0.22).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 140.0 * exp(-18.0 * t) + 40.0
            val env = exp(-10.0 * t)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.85).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGlitch(): ShortArray {
        val numSamples = (sampleRate * 0.15).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i % 250 < 125) 1200.0 else 600.0
            val env = exp(-12.0 * t)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.6).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSweep(): ShortArray {
        val numSamples = (sampleRate * 0.25).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = 300.0 + 1200.0 * (t / 0.25)
            val env = (1.0 - (t / 0.25)) * (t / 0.05).coerceAtMost(1.0)
            val sample = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE * 0.7).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChime(): ShortArray {
        val numSamples = (sampleRate * 0.4).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-6.0 * t)
            val s1 = sin(2.0 * PI * 1046.5 * t) // C6
            val s2 = sin(2.0 * PI * 1318.5 * t) * 0.6 // E6
            val s3 = sin(2.0 * PI * 1567.98 * t) * 0.4 // G6
            val sample = ((s1 + s2 + s3) * env * Short.MAX_VALUE * 0.4).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}

enum class SoundType(val label: String, val padName: String) {
    CLICK("Tick", "PAD 1"),
    BLEEP_HIGH("High Ping", "PAD 2"),
    BLEEP_MID("Mid Beep", "PAD 3"),
    BLEEP_LOW("Low Pop", "PAD 4"),
    KICK("Punch Kick", "PAD 5"),
    GLITCH("Cyber Glitch", "PAD 6"),
    SWEEP("Laser Sweep", "PAD 7"),
    NOTHING_CHIME("Poly Chime", "PAD 8")
}
