package com.ivan.mipc2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {

    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        require(apiKey.isNotBlank()) {
            "API Key de Gemini no configurada"
        }

        val url = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        )

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true

        val body = JSONObject()
            .put(
                "contents",
                org.json.JSONArray()
                    .put(
                        JSONObject()
                            .put(
                                "parts",
                                org.json.JSONArray()
                                    .put(JSONObject().put("text", prompt))
                            )
                    )
            )
            .toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val response = stream.bufferedReader().use { it.readText() }

        if (responseCode !in 200..299) {
            throw Exception("Gemini HTTP $responseCode: $response")
        }

        val json = JSONObject(response)

        json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}
