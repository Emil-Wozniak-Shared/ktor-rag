package pl.model.redis

import io.ktor.http.*
import kotlinx.serialization.Serializable
import pl.model.error.Failure
import pl.model.error.FailureResponse

@Serializable
object CannotFindDocuments : Failure {
    override val message: String = "Cannot find documents"
}

@Serializable
data class DocumentFailureResponse(
    override val message: String,
): FailureResponse {
    override fun status(): HttpStatusCode = HttpStatusCode.InternalServerError
}

val documentsFailureResponseMapper: (failure: Failure) -> DocumentFailureResponse = {
    DocumentFailureResponse(message = it.message)
}
