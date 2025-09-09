package com.c168.aaryahi

import com.google.ai.client.generativeai.Chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.function.BiFunction

object GeminiHelper {
    private val scope = CoroutineScope(Dispatchers.Main)

    @JvmStatic
    fun generateText(
        chat: Chat,
        prompt: String,
        callback: BiFunction<String?, Exception?, Void?>
    ) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    chat.sendMessage(prompt)
                }
                callback.apply(response.text, null)
            } catch (e: Exception) {
                callback.apply(null, e)
            }
        }
    }
}
