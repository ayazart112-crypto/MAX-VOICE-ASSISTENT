package com.allen.max.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIHelper {

    private const val API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent"
    private val client = OkHttpClient()
    private val conversationHistory = mutableListOf<JSONObject>()

    fun askAI(userMessage: String, apiKey: String, callback: (String) -> Unit) {
        try {
            // Add user message to history
            conversationHistory.add(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })

            val systemPrompt = """You are MAX (Multilingual Assistant X), a highly advanced voice assistant created by Allen.
                Your personality: Friendly, witty, slightly futuristic, and extremely helpful.
                Language rules:
                1. Always respond in the SAME language the user uses (English or Urdu/Roman Urdu).
                2. If the user mixes languages, respond in English but acknowledge the Urdu parts.
                Conciseness: Keep responses under 20 words as they will be spoken aloud.
                Context: You are running on an Android device and can help with phone tasks. 
                If you cannot perform a task directly, suggest the voice command to use (e.g., "Say 'Open WhatsApp' to chat")."""

            val requestBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("contents", JSONArray(conversationHistory))
            }

            val request = Request.Builder()
                .url("$API_URL?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Sorry, I couldn't connect to AI. Please check internet.")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        callback("AI Error (${response.code}): ${if (response.code == 403) "Invalid API Key" else "Internal Error"}")
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

                        // Add AI response to history
                        conversationHistory.add(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().put(JSONObject().put("text", text)))
                        })

                        callback(text)
                    } catch (e: Exception) {
                        callback("I'm sorry, I couldn't process the AI response. Please try again.")
                    }
                }
            })
        } catch (e: Exception) {
            callback("I'm having trouble connecting to the network. Please check your internet connection.")
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
