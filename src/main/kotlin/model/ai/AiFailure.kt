package pl.model.ai

import kotlinx.serialization.Serializable
import pl.model.error.Failure

interface AiFailure: Failure

@Serializable
object AiNoResponse: AiFailure {
    override val message: String = "AI failed: no response"
}

@Serializable
object NoEmbeddingReceivedResponse: AiFailure {
    override val message: String = "No embedding data received"
}

@Serializable
class OpenAIAPIErrorResponse(
    val status: String,
    val errorBody: String
): AiFailure {
    override val message: String = "OpenAI API error status: $status - $errorBody"
}

