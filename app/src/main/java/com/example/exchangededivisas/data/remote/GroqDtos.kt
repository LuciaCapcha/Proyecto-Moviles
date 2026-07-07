package com.example.exchangededivisas.data.remote

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Double = 0.2,
    @SerializedName("max_tokens") val maxTokens: Int = 220
)

data class GroqMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class GroqChatResponse(
    @SerializedName("choices") val choices: List<GroqChoice>?
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage?
)
