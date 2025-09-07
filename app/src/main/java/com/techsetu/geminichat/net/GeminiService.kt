package com.techsetu.geminichat.net

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}
