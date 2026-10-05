package com.ivan.mipc2

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveClient(
    private val apiKey: String,
    private val onUserTranscript: (String) -> Unit,
    private val onAssistantTranscript: (String) -> Unit,
    private val onState: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val running = AtomicBoolean(false)
    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var recordThread: Thread? = null

    private val sampleIn = 16000
    private val sampleOut = 24000

    fun start() {
        if (running.getAndSet(true)) return

        val client = OkHttpClient()

        val url =
            "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
            "?key=$apiKey"

        socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    android.util.Log.d("MIPC_LIVE", "WebSocket OPEN: ${response.code}")
                    onState("Conectando…")

                    val setup = JSONObject()
                        .put(
                            "setup",
                            JSONObject()
                                .put("model", "models/gemini-3.8-live")
                                .put(
                                    "generationConfig",
                                    JSONObject()
                                        .put("responseModalities", JSONArray().put("AUDIO"))
                                        .put(
                                            "speechConfig",
                                            JSONObject().put(
                                                "voiceConfig",
                                                JSONObject().put(
                                                    "prebuiltVoiceConfig",
                                                    JSONObject().put("voiceName", "Aoede")
                                                )
                                            )
                                        )
                                )
                                .put(
                                    "systemInstruction",
                                    JSONObject().put(
                                        "parts",
                                        JSONArray().put(
                                            JSONObject().put(
                                                "text",
                                                "Sos Mi PC, un asistente de voz en español argentino. " +
                                                "Respondé de forma natural, breve y útil. " +
                                                "La conversación es en tiempo real."
                                            )
                                        )
                                    )
                                )
                                .put("inputAudioTranscription", JSONObject())
                                .put("outputAudioTranscription", JSONObject())
                        )

                    android.util.Log.d("MIPC_LIVE", "Enviando setup")
                    webSocket.send(setup.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = JSONObject(text)

                        if (root.has("setupComplete")) {
                            android.util.Log.d("MIPC_LIVE", "SETUP COMPLETE")
                            onState("Escuchando…")
                            startAudioRecorder()
                        }

                        val server = root.optJSONObject("serverContent") ?: return

                        if (server.optBoolean("interrupted", false)) {
                            player?.pause()
                            player?.flush()
                        }

                        val input = server.optJSONObject("inputTranscription")
                        if (input != null) {
                            val value = input.optString("text")
                            if (value.isNotBlank()) onUserTranscript(value)
                        }

                        val output = server.optJSONObject("outputTranscription")
                        if (output != null) {
                            val value = output.optString("text")
                            if (value.isNotBlank()) onAssistantTranscript(value)
                        }

                        val modelTurn = server.optJSONObject("modelTurn")
                        val parts = modelTurn?.optJSONArray("parts")

                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val part = parts.optJSONObject(i) ?: continue
                                val inline = part.optJSONObject("inlineData") ?: continue
                                val data = inline.optString("data")
                                if (data.isNotBlank()) playAudio(data)
                            }
                        }

                        if (server.optBoolean("turnComplete", false)) {
                            onState("Escuchando…")
                        }
                    } catch (e: Exception) {
                        onError(e.message ?: "Error procesando respuesta")
                    }
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    running.set(false)
                    android.util.Log.e(
                        "MIPC_LIVE",
                        "WebSocket FAILURE: ${t.message} HTTP=${response?.code}",
                        t
                    )
                    onError(t.message ?: "Conexión Live perdida")
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    running.set(false)
                }
            }
        )
    }

    private fun startAudioRecorder() {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleIn,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleIn,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, sampleIn / 2)
        )

        recorder?.startRecording()

        recordThread = Thread {
            val buffer = ByteArray(3200)

            while (running.get()) {
                val count = recorder?.read(buffer, 0, buffer.size) ?: -1

                if (count > 0) {
                    val encoded = Base64.encodeToString(
                        buffer.copyOf(count),
                        Base64.NO_WRAP
                    )

                    val audio = JSONObject()
                        .put(
                            "audio",
                            JSONObject()
                                .put("data", encoded)
                                .put("mimeType", "audio/pcm;rate=16000")
                        )

                    socket?.send(
                        JSONObject()
                            .put("realtimeInput", JSONObject().put("audio", audio.getJSONObject("audio")))
                            .toString()
                    )
                }
            }
        }

        recordThread?.start()
    }

    private fun playAudio(base64: String) {
        try {
            if (player == null) {
                val minBuffer = AudioTrack.getMinBufferSize(
                    sampleOut,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                player = AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleOut,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, sampleOut / 2),
                    AudioTrack.MODE_STREAM
                )

                player?.play()
            }

            val pcm = Base64.decode(base64, Base64.DEFAULT)
            player?.write(pcm, 0, pcm.size)
        } catch (e: Exception) {
            onError(e.message ?: "Error reproduciendo audio")
        }
    }

    fun close() {
        running.set(false)

        try {
            recorder?.stop()
        } catch (_: Exception) {}

        try {
            recorder?.release()
        } catch (_: Exception) {}

        recorder = null

        try {
            player?.stop()
        } catch (_: Exception) {}

        try {
            player?.release()
        } catch (_: Exception) {}

        player = null

        socket?.close(1000, "closed")
        socket = null
    }
}
