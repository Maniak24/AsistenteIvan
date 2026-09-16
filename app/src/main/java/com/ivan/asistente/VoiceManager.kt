package com.ivan.asistente

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class VoiceManager(private val context: Context) {

    private var tts: OfflineTts? = null
    private var currentTrack: AudioTrack? = null
    private val modelDir = File(context.filesDir, "voz")

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (tts != null) return@withContext

        copyAssets("voz", modelDir)

        val modelFile = File(modelDir, "es_AR-daniela-high.onnx")
        val tokensFile = File(modelDir, "tokens.txt")
        val dataDir = File(modelDir, "espeak-ng-data")

        check(modelFile.exists()) { "No se encontró el modelo de Daniela." }
        check(tokensFile.exists()) { "No se encontró tokens.txt de Daniela." }
        check(dataDir.exists()) { "No se encontró espeak-ng-data de Daniela." }

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelFile.absolutePath,
                    tokens = tokensFile.absolutePath,
                    dataDir = dataDir.absolutePath
                ),
                numThreads = 2,
                debug = false
            )
        )

        tts = OfflineTts(context.assets, config)
    }

    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext
        if (tts == null) initialize()
        val engine = tts ?: return@withContext

        // Dividir respuestas largas evita generar un bloque de audio enorme
        // de una sola vez y permite que Daniela empiece a hablar antes.
        val chunks = text
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .let { if (it.isEmpty()) listOf(text.trim()) else it }

        for (chunk in chunks) {
            coroutineContext.ensureActive()
            val audio = engine.generate(
                text = chunk,
                sid = 0,
                speed = 1.0f
            )
            playAudio(audio.samples, audio.sampleRate)
        }
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        if (samples.isEmpty()) return

        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            pcm[i] = (samples[i] * 32767f)
                .coerceIn(-32768f, 32767f)
                .roundToInt()
                .toShort()
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        currentTrack = audioTrack
        try {
            audioTrack.play()

            var offset = 0
            while (offset < pcm.size) {
                coroutineContext.ensureActive()
                val count = minOf(4096, pcm.size - offset)
                val written = audioTrack.write(pcm, offset, count, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }

            while (audioTrack.playbackHeadPosition < pcm.size) {
                coroutineContext.ensureActive()
                Thread.sleep(15)
            }
        } finally {
            runCatching { audioTrack.stop() }
            runCatching { audioTrack.flush() }
            audioTrack.release()
            if (currentTrack === audioTrack) currentTrack = null
        }
    }

    fun stop() {
        runCatching { currentTrack?.pause() }
        runCatching { currentTrack?.flush() }
        runCatching { currentTrack?.stop() }
        currentTrack = null
    }

    fun release() {
        stop()
        tts?.release()
        tts = null
    }

    private fun copyAssets(assetPath: String, destination: File) {
        val assetManager = context.assets
        val entries = assetManager.list(assetPath) ?: return

        destination.mkdirs()

        for (entry in entries) {
            val sourcePath = "$assetPath/$entry"
            val target = File(destination, entry)
            val children = assetManager.list(sourcePath)

            if (children != null && children.isNotEmpty()) {
                copyAssets(sourcePath, target)
            } else {
                assetManager.open(sourcePath).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
