package com.kitagawh.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny Groq API client using only HttpURLConnection + org.json.
 * Chat answers power general questions; the vision model powers
 * Screen Search by describing a screenshot.
 */
object GroqClient {

    private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val CHAT_MODEL = "llama-3.3-70b-versatile"
    private const val VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"

    const val NO_KEY_MESSAGE =
        "I need a brain for that. Add your free Groq API key in Settings, in the API section, and I will answer anything."

    fun chat(apiKey: String, userText: String, callback: (String) -> Unit) {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        "You are Kitagawh, a friendly phone assistant living inside the user's Android phone. " +
                            "Answer briefly, in one or two short spoken sentences, in simple warm language."
                    )
            )
            .put(JSONObject().put("role", "user").put("content", userText))
        request(apiKey, CHAT_MODEL, messages, callback)
    }

    fun describeScreen(apiKey: String, base64Jpeg: String, callback: (String) -> Unit) {
        val content = JSONArray()
            .put(
                JSONObject()
                    .put("type", "text")
                    .put(
                        "text",
                        "Explain clearly what is on this phone screen: what app or page it is, " +
                            "the main things visible, and anything the user may want to know. Keep it under 80 words."
                    )
            )
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg")
                    )
            )
        val messages = JSONArray()
            .put(JSONObject().put("role", "user").put("content", content))
        request(apiKey, VISION_MODEL, messages, callback)
    }

    private fun request(
        apiKey: String,
        model: String,
        messages: JSONArray,
        callback: (String) -> Unit
    ) {
        Thread {
            try {
                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", 0.6)
                    .put("max_tokens", 400)

                val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = BufferedReader(InputStreamReader(stream)).readText()
                if (code !in 200..299) {
                    callback("The Groq API said no (error $code). Check your API key in Settings.")
                    return@Thread
                }
                val reply = JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
                callback(reply.ifBlank { "I got an empty answer. Try asking again." })
            } catch (e: Exception) {
                callback("I could not reach the AI brain. Check your internet and try again.")
            }
        }.start()
    }
}
