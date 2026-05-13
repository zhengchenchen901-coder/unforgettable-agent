package com.unforgettable.memory.data.llm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface LlmChatService {
    @POST
    suspend fun createChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest,
    ): ChatCompletionResponse

    companion object {
        fun create(): LlmChatService {
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(LlmChatService::class.java)
        }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int = 800,
    @SerialName("response_format")
    val responseFormat: ChatResponseFormat? = ChatResponseFormat(),
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatResponseFormat(
    val type: String = "json_object",
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val message: ChatChoiceMessage? = null,
)

@Serializable
data class ChatChoiceMessage(
    val role: String? = null,
    val content: String? = null,
)
