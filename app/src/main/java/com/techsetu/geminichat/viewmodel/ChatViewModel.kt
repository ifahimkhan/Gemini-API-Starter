package com.techsetu.geminichat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.techsetu.geminichat.data.Message
import com.techsetu.geminichat.repo.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ChatRepository(app)

    val messages: LiveData<List<Message>> = repo.messages()

    fun sendText(text: String) {
        viewModelScope.launch {
            repo.sendMessage(text)
        }
    }
}
