package com.ivan.mipc2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    private val models = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash"
    )

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()

        if (apiKey.isBlank()) {
            throw Exception("La clave de Gemini no llegó a la APK.")
        }

        var lastError = "Gemini no respondió."

        for (model in models) {
            try {
                return@withContext request(model, prompt, apiKey)
            } catch (e: Exception) {
                lastError = e.message ?: lastError

                if (!lastError.contains("HTTP 404")) {
                    throw e
                }
            }
        }

        throw Exception(lastError)
    }

    private fun request(
        model: String,
        prompt: String,
        apiKey: String
    ): String {

        val connection = (URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        ).openConnection() as HttpURLConnection).apply {

            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)

            connectTimeout = 15000
            readTimeout = 45000
            doOutput = true
        }

        try {
            val body = JSONObject()
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", prompt)
                                )
                            )
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("maxOutputTokens", 2048)
                )
                .toString()

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode

            val stream =
                if (code in 200..299)
                    connection.inputStream
                else
                    connection.errorStream

            val response =
                stream?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("Gemini no devolvió respuesta.")

            if (code !in 200..299) {
                throw Exception("Gemini HTTP $code")
            }

            val json = JSONObject(response)

            val candidates =
                json.optJSONArray("candidates")
                    ?: throw Exception("Gemini no devolvió candidatos.")

            if (candidates.length() == 0) {
                throw Exception("Gemini devolvió una respuesta vacía.")
            }

            val parts =
                candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?: throw Exception("Respuesta de Gemini sin contenido.")

            val result = buildString {
                for (i in 0 until parts.length()) {
                    append(parts.getJSONObject(i).optString("text"))
                }
            }.trim()

            if (result.isBlank()) {
                throw Exception("Gemini devolvió texto vacío.")
            }

            return result

        } finally {
            connection.disconnect()
        }
    }
}
