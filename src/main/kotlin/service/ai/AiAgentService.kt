package pl.service.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.Serializable
import pl.model.ai.AiFailure
import pl.model.ai.AiNoResponse
import pl.model.ai.NoEmbeddingReceivedResponse
import pl.model.ai.OpenAIAPIErrorResponse
import kotlin.math.sqrt

interface AiAgentService {
    suspend fun ask(input: String): Either<AiFailure, String>
    suspend fun embeddings(text: String): Either<AiFailure, List<Float>>
    suspend fun processText(
        prompt: String,
        systemMessage: String?,
        model: String,
        maxTokens: Int?,
        temperature: Double?
    ): Result<String>
}

private val WORD_REGEX = "\\W+".toRegex()

internal class OpenAiAgentService(
    private val config: ApplicationConfig,
    private val client: HttpClient
) : AiAgentService {
    private val logger = KotlinLogging.logger {}

    private val apiToken = config.property("koog.api-key.openai").getString()
    private val embeddingModel = "text-embedding-3-small"

    private val agent = AIAgent(
        executor = simpleOpenAIExecutor(apiToken),
        systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
        llmModel = OpenAIModels.Chat.GPT4o
    )

    override suspend fun ask(input: String): Either<AiFailure, String> = either {
        logger.info { "Ask OpenAI: $input" }
        val result = agent.runAndGetResult(input)
        ensure(result != null) {
            logger.error { "No response from OpenAI" }
            raise(AiNoResponse)
        }
        result
    }

    override suspend fun embeddings(text: String): Either<AiFailure, List<Float>> = either {
        try {
            logger.info { "Embeddings OpenAI: $text" }
            val response = client.post("https://api.openai.com/v1/embeddings") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(OpenAIEmbeddingRequest(input = text, model = embeddingModel))
            }
            if (response.status == HttpStatusCode.OK) {
                val embedding = response.body<OpenAIEmbeddingResponse>().data.firstOrNull()?.embedding
                ensure(embedding != null) {
                    logger.error { "OpenAI: No embedding data received" }
                    raise(NoEmbeddingReceivedResponse)
                }
                logger.info { "Embedding response: $embedding" }
                embedding
            } else {
                val errorBody = response.body<String>()
                logger.error { "OpenAI: $errorBody" }
                raise(OpenAIAPIErrorResponse(response.status.description, errorBody))
            }
        } catch (e: Exception) {
            logger.error { "Error generating embedding: ${e.message}" }
            createMockEmbedding(text)
        }
    }

    private fun createMockEmbedding(text: String, dimensions: Int = 1536): List<Float> {
        val words = text.lowercase().split(WORD_REGEX).filter { it.isNotEmpty() }
        val embedding = FloatArray(dimensions) { 0f }

        words.forEachIndexed { index, word ->
            val hash = word.hashCode()
            (0 until dimensions).forEach { i ->
                embedding[i] += (hash shr (i % 32) and 1).toFloat() * (1f / sqrt(words.size.toFloat()))
            }
        }
        val magnitude = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (magnitude > 0) {
            embedding.indices.forEach { i ->
                embedding[i] /= magnitude
            }
        }

        return embedding.toList()
    }

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

    suspend fun chatCompletion(
        messages: List<pl.model.ai.OpenAIMessage>, model: String = "gpt-4", maxTokens: Int? = null, temperature: Double? = null
    ): Result<pl.model.ai.OpenAIResponse> {
        return try {
            val request = _root_ide_package_.pl.model.ai.OpenAIRequest(
                model = model, messages = messages, max_tokens = maxTokens, temperature = temperature
            )

            val response = client.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val openAIResponse = response.body<pl.model.ai.OpenAIResponse>()
                Result.success(openAIResponse)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("OpenAI API error: ${response.status} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processText(
        prompt: String,
        systemMessage: String?,
        model: String,
        maxTokens: Int?,
        temperature: Double?
    ): Result<String> {
        val messages = mutableListOf<pl.model.ai.OpenAIMessage>()

        systemMessage?.let {
            messages.add(_root_ide_package_.pl.model.ai.OpenAIMessage("system", it))
        }
        messages.add(_root_ide_package_.pl.model.ai.OpenAIMessage("user", prompt))

        return chatCompletion(messages, model, maxTokens, temperature).map { response ->
            response.choices.firstOrNull()?.message?.content ?: throw Exception("No response content from OpenAI")
        }
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
    val usage: pl.model.ai.OpenAIUsage
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