package com.allen.max.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIHelper {

    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    private val client = OkHttpClient()
    private val conversationHistory = mutableListOf<JSONObject>()

    fun askAI(userMessage: String, apiKey: String, callback: (String) -> Unit) {
        try {
            // Add user message to history
            conversationHistory.add(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            })

            val systemPrompt = """You are MAX, an intelligent voice assistant created by Allen. 
                You are helpful, friendly and concise. Keep responses short (1-2 sentences) 
                since they will be spoken aloud. You can understand both English and Urdu.
                If user speaks Urdu, reply in Urdu. If English, reply in English."""

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
                        callback("Sorry, something went wrong.")
                    }
                }
            })
        } catch (e: Exception) {
            callback("Error connecting to AI.")
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
