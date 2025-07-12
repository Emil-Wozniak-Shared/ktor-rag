package pl.service.ai

import arrow.core.Either
import org.apache.commons.math3.linear.ArrayRealVector
import pl.model.ai.AiFailure

interface EmbeddingService {
    suspend fun generateEmbedding(text: String): Either<AiFailure, List<Float>>
    fun cosineSimilarity(a: List<Float>, b: List<Float>): Double
}

internal class EmbeddingServiceImpl(
    private val aiAgentService: AiAgentService
) : EmbeddingService {

    override suspend fun generateEmbedding(text: String): Either<AiFailure, List<Float>> =
        aiAgentService.embeddings(text)

    override fun cosineSimilarity(a: List<Float>, b: List<Float>): Double {
        if (a.size != b.size) return 0.0

        val vecA = ArrayRealVector(a.map { it.toDouble() }.toDoubleArray())
        val vecB = ArrayRealVector(b.map { it.toDouble() }.toDoubleArray())

        return vecA.dotProduct(vecB) / (vecA.norm * vecB.norm)
    }
}