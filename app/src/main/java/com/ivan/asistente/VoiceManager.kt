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
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class VoiceManager(private val context: Context) {

    private var tts: OfflineTts? = null
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

        engine.generate(
            text = text,
            sid = 0,
            speed = 1.0f
        ).let { audio ->
            playAudio(audio.samples, audio.sampleRate)
        }
    }

    private fun playAudio(samples: FloatArray, sampleRate: Int) {
        val pcm = ShortArray(samples.size)

        for (i in samples.indices) {
            pcm[i] = (samples[i] * 32767f)
                .coerceIn(-32768f, 32767f)
                .roundToInt()
                .toShort()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

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
            .setBufferSizeInBytes(maxOf(bufferSize, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.play()
            Thread.sleep((pcm.size * 1000L / sampleRate) + 120)
        } finally {
            runCatching { audioTrack.stop() }
            audioTrack.release()
        }
    }

    fun release() {
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
