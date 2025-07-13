package pl.model.error

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface Failure {
    val message: String
}

interface FailureResponse {
    val message: String
    fun status(): HttpStatusCode
}

abstract class ProblemDetails {
    abstract val type: String
    abstract val title: String
}

@Serializable
data class InvalidParamsProblemDetails(
    override val type: String,
    override val title: String,
    @SerialName("invalid-params")
    val invalidParams: List<InvalidParam>
) : ProblemDetails() {
    constructor(call: ApplicationCall, cause: RequestValidationException) : this(
        call.request.path(),
        "Invalid parameters",
        cause.reasons.map {
            InvalidParam(
                it.substringBefore(">").replace("<", "Incorrect param: "),
                it.substringAfter(">")
            )
        }
    )
}

@Serializable
data class InvalidParam(
    val name: String,
    val reason: String
)