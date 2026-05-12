package com.unforgettable.memory.data.llm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenAIService {
    @POST("responses")
    suspend fun createResponse(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIResponseRequest,
    ): OpenAIResponse

    companion object {
        fun create(): OpenAIService {
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
                .baseUrl("https://api.openai.com/v1/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenAIService::class.java)
        }
    }
}

@Serializable
data class OpenAIResponseRequest(
    val model: String,
    val input: List<OpenAIInputMessage>,
    val text: OpenAITextConfig,
    val temperature: Double = 0.1,
)

@Serializable
data class OpenAIInputMessage(
    val role: String,
    val content: List<OpenAIInputContent>,
)

@Serializable
data class OpenAIInputContent(
    val type: String = "input_text",
    val text: String,
)

@Serializable
data class OpenAITextConfig(
    val format: OpenAIResponseFormat,
)

@Serializable
data class OpenAIResponseFormat(
    val type: String = "json_schema",
    val name: String,
    val strict: Boolean = true,
    val schema: JsonObject,
    val description: String? = null,
)

@Serializable
data class OpenAIResponse(
    val output: List<OpenAIOutputItem> = emptyList(),
)

@Serializable
data class OpenAIOutputItem(
    val content: List<OpenAIOutputContent> = emptyList(),
)

@Serializable
data class OpenAIOutputContent(
    val type: String? = null,
    val text: String? = null,
    @SerialName("refusal")
    val refusal: String? = null,
)

