package com.allen.max.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIHelper {

    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    private val client = OkHttpClient()
    private val conversationHistory = mutableListOf<JSONObject>()

    fun askAI(userMessage: String, apiKey: String, callback: (String) -> Unit) {
        try {
            val systemPrompt = """You are MAX (Multilingual Assistant X), a highly advanced voice assistant created by Allen.
                Your personality: Friendly, witty, slightly futuristic, and extremely helpful.
                Language rules:
                1. Always respond in the SAME language the user uses (English or Urdu/Roman Urdu).
                2. If the user mixes languages, respond in English but acknowledge the Urdu parts.
                Conciseness: Keep responses under 20 words as they will be spoken aloud.
                Context: You are running on an Android device and can help with phone tasks. 
                If you cannot perform a task directly, suggest the voice command to use (e.g., "Say 'Open WhatsApp' to chat")."""

            // Create temporary contents array for this request
            val currentContents = JSONArray()
            conversationHistory.forEach { currentContents.put(it) }
            
            // Add CURRENT user message to the request only
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
                    callback("Sorry, I couldn't connect to AI. Please check internet.")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        if (response.code == 400) {
                            conversationHistory.clear() // Reset on bad request to fix role sequence
                        }
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

                        // COMMIT current turn to history after success
                        conversationHistory.add(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
                        })
                        conversationHistory.add(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().put(JSONObject().put("text", text)))
                        })

                        if (conversationHistory.size > 20) {
                            conversationHistory.removeAt(0)
                            conversationHistory.removeAt(0)
                        }

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
