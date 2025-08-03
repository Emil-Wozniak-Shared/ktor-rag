package pl.model.ai

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false
)

@Serializable
data class OpenAIMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: pl.service.ai.OpenAIUsage? = null
)

@Serializable
data class OpenAIChoice(
    val index: Int, val message: OpenAIMessage, val finish_reason: String?
)

@Serializable
data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
