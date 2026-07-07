package com.example.exchangededivisas.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface GroqService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body body: GroqChatRequest
    ): GroqChatResponse
}
