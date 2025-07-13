package pl.model.ai

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import pl.model.DtoResponse
import pl.model.error.Failure
import pl.model.error.FailureResponse

@Serializable
class AskAiResponse(val message: String) : DtoResponse

interface AiFailureResponse : FailureResponse {
    override val message: String
}

val aiFailureMapper: (Failure) -> AiFailureResponse = {
    object : AiFailureResponse {
        override val message: String = it.message
        override fun status(): HttpStatusCode = HttpStatusCode.InternalServerError
    }
}

val askAiResponseMapper: (message: String) -> AskAiResponse = { AskAiResponse(message = it) }

