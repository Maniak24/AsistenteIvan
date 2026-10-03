package com.ivan.mipc2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank()) {
            throw Exception("La clave de Gemini no llegó a la APK.")
        }

        val connection = (URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
        }

        try {
            val body = JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put("text", prompt)
                            )
                        )
                    )
                )
                .toString()

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use { it.readText() }
                ?: throw Exception("Gemini no devolvió respuesta.")

            if (code !in 200..299) {
                throw Exception("Gemini HTTP $code")
            }

            val json = JSONObject(response)

            val candidates = json.optJSONArray("candidates")
                ?: throw Exception("Gemini no devolvió candidatos.")

            if (candidates.length() == 0) {
                throw Exception("Gemini devolvió una respuesta vacía.")
            }

            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw Exception("Respuesta de Gemini sin contenido.")

            if (parts.length() == 0) {
                throw Exception("Gemini devolvió contenido vacío.")
            }

            parts.getJSONObject(0)
                .optString("text")
                .ifBlank {
                    throw Exception("Gemini no devolvió texto.")
                }

        } finally {
            connection.disconnect()
        }
    }
}
