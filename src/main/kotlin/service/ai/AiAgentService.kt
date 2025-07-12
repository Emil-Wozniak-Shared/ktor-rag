package pl.service.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import pl.model.ai.AiFailure
import pl.model.ai.AiNoResponse
import pl.model.ai.NoEmbeddingReceivedResponse
import pl.model.ai.OpenAIAPIErrorResponse
import kotlin.math.sqrt

interface AiAgentService {
    suspend fun ask(input: String): Either<AiFailure, String>
    suspend fun embeddings(text: String): Either<AiFailure, List<Float>>
}

internal class OpenAiAgentService(
    private val aiApikey: AIApiKey,
    private val client: HttpClient
) : AiAgentService {
    val agent = AIAgent(
        executor = simpleOpenAIExecutor(aiApikey.openAi),
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = OpenAIModels.Chat.GPT4o
    )

    override suspend fun ask(input: String): Either<AiFailure, String> = either {
        val result = agent.runAndGetResult(input)
        ensure(result != null) {
            raise(AiNoResponse)
        }
        result
    }

    private val embeddingModel = "text-embedding-3-small"

    override suspend fun embeddings(text: String): Either<AiFailure, List<Float>> = either {
        try {
            val response = client.post("https://api.openai.com/v1/embeddings") {
                header(HttpHeaders.Authorization, "Bearer $aiApikey")
                header(HttpHeaders.ContentType, "application/json")
                setBody(OpenAIEmbeddingRequest(input = text, model = embeddingModel))
            }
            if (response.status == HttpStatusCode.OK) {
                val embedding = response.body<OpenAIEmbeddingResponse>().data.firstOrNull()?.embedding
                ensure(embedding != null) {
                    raise(NoEmbeddingReceivedResponse)
                }
                embedding
            } else {
                val errorBody = response.body<String>()
                raise(OpenAIAPIErrorResponse(response.status.description, errorBody))
            }
        } catch (e: Exception) {
            println("Error generating embedding: ${e.message}")
            createMockEmbedding(text)
        }
    }

    private fun createMockEmbedding(text: String, dimensions: Int = 1536): List<Float> {
        val words = text.lowercase().split(Regex("\\W+")).filter { it.isNotEmpty() }
        val embedding = FloatArray(dimensions) { 0f }

        words.forEachIndexed { index, word ->
            val hash = word.hashCode()
            (0 until dimensions).forEach { i ->
                embedding[i] += (hash shr (i % 32) and 1).toFloat() * (1f / sqrt(words.size.toFloat()))
            }
        }

        // Normalize
        val magnitude = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (magnitude > 0) {
            embedding.indices.forEach { i ->
                embedding[i] /= magnitude
            }
        }

        return embedding.toList()
    }

    // Get model info
    fun getModelInfo(): EmbeddingModelInfo = when (embeddingModel) {
        "text-embedding-3-small" -> EmbeddingModelInfo(
            name = "text-embedding-3-small",
            dimensions = 1536,
            maxTokens = 8192,
            pricePerMillion = 0.02
        )

        "text-embedding-3-large" -> EmbeddingModelInfo(
            name = "text-embedding-3-large",
            dimensions = 3072,
            maxTokens = 8192,
            pricePerMillion = 0.13
        )

        "text-embedding-ada-002" -> EmbeddingModelInfo(
            name = "text-embedding-ada-002",
            dimensions = 1536,
            maxTokens = 8192,
            pricePerMillion = 0.10
        )

        else -> EmbeddingModelInfo(embeddingModel, 1536, 8192, 0.0)
    }
}

@Serializable
data class OpenAIEmbeddingRequest(
    val input: String,
    val model: String,
    val encoding_format: String = "float"
)

@Serializable
data class OpenAIEmbeddingResponse(
    val `object`: String,
    val data: List<OpenAIEmbeddingData>,
    val model: String,
    val usage: OpenAIUsage
)

@Serializable
data class OpenAIEmbeddingData(
    val `object`: String,
    val embedding: List<Float>,
    val index: Int
)

@Serializable
data class OpenAIUsage(
    val prompt_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class EmbeddingModelInfo(
    val name: String,
    val dimensions: Int,
    val maxTokens: Int,
    val pricePerMillion: Double
)