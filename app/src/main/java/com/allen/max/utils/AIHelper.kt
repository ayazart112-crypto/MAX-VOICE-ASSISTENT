package com.allen.max.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIHelper {

    const val GEMINI_KEY = com.allen.max.BuildConfig.GEMINI_KEY
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    private val client = OkHttpClient()

    // FIX: synchronized list to prevent thread-safety crashes
    private val conversationHistory = mutableListOf<JSONObject>()
    private val historyLock = Any()

    fun askAI(userMessage: String, apiKey: String = GEMINI_KEY, callback: (String) -> Unit) {
        // FIX: validate API key before any network call
        if (apiKey.isNullOrBlank() || apiKey == "null") {
            callback("AI brain offline. Please add your Gemini API key to local.properties")
            return
        }

        try {
            val systemPrompt = """You are MAX (Multilingual Assistant X), a highly advanced voice assistant created by Allen.
                Your personality: Friendly, witty, slightly futuristic, and extremely helpful.
                Language rules:
                1. Always respond in the SAME language the user uses (English or Urdu/Roman Urdu).
                2. If the user mixes languages, respond in English but acknowledge the Urdu parts.
                Conciseness: Keep responses under 20 words as they will be spoken aloud.
                Context: You are running on an Android device and can help with phone tasks.
                If you cannot perform a task directly, suggest the voice command to use (e.g., "Say 'Open WhatsApp' to chat")."""

            // FIX: build request contents safely under lock
            val currentContents = JSONArray()
            synchronized(historyLock) {
                conversationHistory.forEach { currentContents.put(it) }
            }
            currentContents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })

            val requestBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("contents", currentContents)
            }

            val request = Request.Builder()
                .url("$API_URL?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Sorry, I couldn't connect to AI. Please check your internet.")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        // FIX: clear history on 400 (bad role sequence), give clear error messages
                        if (response.code == 400) {
                            synchronized(historyLock) { conversationHistory.clear() }
                        }
                        val reason = when (response.code) {
                            400 -> "Conversation reset. Please try again."
                            401, 403 -> "Invalid API key. Check gemini_key.txt"
                            429 -> "Too many requests. Please wait a moment."
                            500, 503 -> "Gemini service is down. Try again later."
                            else -> "AI error (${response.code}). Try again."
                        }
                        callback(reason)
                        return
                    }

                    try {
                        val json = JSONObject(body)
                        val text = json
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")

                        // FIX: only commit to history on success, always in pairs
                        synchronized(historyLock) {
                            conversationHistory.add(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                            })
                            conversationHistory.add(JSONObject().apply {
                                put("role", "model")
                                put("parts", JSONArray().put(JSONObject().put("text", text)))
                            })
                            // FIX: keep last 10 turns max, always remove in pairs
                            while (conversationHistory.size > 20) {
                                conversationHistory.removeAt(0)
                                conversationHistory.removeAt(0)
                            }
                        }

                        callback(text)
                    } catch (e: Exception) {
                        callback("I couldn't read the AI response. Please try again.")
                    }
                }
            })
        } catch (e: Exception) {
            callback("Network error. Please check your internet connection.")
        }
    }

    fun clearHistory() {
        synchronized(historyLock) { conversationHistory.clear() }
    }
}
