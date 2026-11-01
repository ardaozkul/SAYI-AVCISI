package com.sayiavcisi.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Harici ses dosyalarına veya internet bağlantısına ihtiyaç duymadan,
 * gerçek zamanlı PCM ses senteziyle (AudioTrack) çalışan çevrimdışı ses efektleri yöneticisi:
 * - Doğru cevap sesi (neşeli yükselen akor)
 * - Yanlış cevapta yumuşak uyarı (nazik düşük frekanslı çift ton)
 * - Bonus / Joker / Altın kazanma sesi (parlak arpej)
 * - Bölüm tamamlama müziği (kutlama fanfar melodisi)
 */
object SoundEffectsManager {

    @Volatile
    var isSoundEnabled: Boolean = true

    fun playCorrectSound() {
        if (!isSoundEnabled) return
        playMelodyAsync(
            listOf(
                Note(523.25, 85), // C5
                Note(659.25, 85), // E5
                Note(783.99, 140) // G5
            )
        )
    }

    fun playGentleWrongSound() {
        if (!isSoundEnabled) return
        playMelodyAsync(
            listOf(
                Note(349.23, 110), // F4 (yumuşak)
                Note(311.13, 150)  // D#4
            ),
            volumeScale = 0.22f
        )
    }

    fun playBonusSound() {
        if (!isSoundEnabled) return
        playMelodyAsync(
            listOf(
                Note(587.33, 70),  // D5
                Note(739.99, 70),  // F#5
                Note(880.00, 70),  // A5
                Note(1174.66, 160) // D6
            )
        )
    }

    fun playLevelCompleteFanfare() {
        if (!isSoundEnabled) return
        playMelodyAsync(
            listOf(
                Note(523.25, 100), // C5
                Note(659.25, 100), // E5
                Note(783.99, 100), // G5
                Note(1046.50, 180), // C6
                Note(783.99, 90),  // G5
                Note(1046.50, 280) // C6
            )
        )
    }

    private data class Note(val freqHz: Double, val durationMs: Int)

    private fun playMelodyAsync(notes: List<Note>, volumeScale: Float = 0.35f) {
        thread(isDaemon = true) {
            try {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (it.durationMs * sampleRate) / 1000 }
                if (totalSamples <= 0) return@thread

                val pcmBuffer = ShortArray(totalSamples)
                var offset = 0

                for (note in notes) {
                    val count = (note.durationMs * sampleRate) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val envelope = exp(-3.5 * (i.toDouble() / count))
                        val wave = sin(2.0 * PI * note.freqHz * t) + 0.25 * sin(4.0 * PI * note.freqHz * t)
                        val sample = (wave * envelope * volumeScale * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        if (offset < pcmBuffer.size) {
                            pcmBuffer[offset++] = sample.toShort()
                        }
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(streamBufferSizeInBytes(sampleRate, pcmBuffer.size))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                try {
                    audioTrack.play()
                    val chunk = ShortArray(minOf(CHUNK_SAMPLES, pcmBuffer.size))
                    var written = 0
                    while (written < pcmBuffer.size) {
                        val sampleCount = minOf(chunk.size, pcmBuffer.size - written)
                        System.arraycopy(pcmBuffer, written, chunk, 0, sampleCount)
                        val progress = audioTrack.write(chunk, 0, sampleCount)
                        if (progress <= 0) break
                        written += progress
                    }
                    Thread.sleep(120L)
                    audioTrack.stop()
                } finally {
                    audioTrack.release()
                }
            } catch (_: Throwable) {
                // Ses donanımı meşgulse sessizce geç
            }
        }
    }

    private const val CHUNK_SAMPLES = 2048

    private fun streamBufferSizeInBytes(sampleRate: Int, pcmSampleCount: Int): Int {
        val minBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)
        return maxOf(minBytes, pcmSampleCount * 2)
    }
}
