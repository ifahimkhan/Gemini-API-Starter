package com.techsetu.geminichat.repo

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.techsetu.geminichat.R
import com.techsetu.geminichat.data.AppDatabase
import com.techsetu.geminichat.data.Message
import com.techsetu.geminichat.net.GeminiContent
import com.techsetu.geminichat.net.GeminiRequest
import com.techsetu.geminichat.net.GeminiService
import com.techsetu.geminichat.net.GeminiTextPart
import com.techsetu.geminichat.net.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(private val app: Application) {
    private val dao = AppDatabase.get(app).messageDao()
    private val service: GeminiService = NetworkModule.service()
    private val apiKey: String = app.getString(R.string.gemini_api_key)

    fun messages(): LiveData<List<Message>> = dao.messages().asLiveData()

    suspend fun sendMessage(text: String) {
        // Save user message
        dao.insert(Message(text = text, isUser = true))

        // Call Gemini
        val req = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiTextPart(text)))
            )
        )

        val response = withContext(Dispatchers.IO) {
            service.generateContent(apiKey = apiKey, body = req)
        }

        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: "No response"

        dao.insert(Message(text = reply, isUser = false))
    }
}
