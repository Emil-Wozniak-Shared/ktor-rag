package pl.ext

import arrow.core.Either
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.model.DtoResponse
import pl.model.error.Failure
import pl.model.error.FailureResponse

inline fun <reified F, reified T, reified R> Either<Failure, T>.toDto(
    failureMapper: (Failure) -> F,
    entityMapper: (T) -> R
): Either<F, R>
        where T : Any,
              F : FailureResponse,
              R : DtoResponse =
    this
        .map(entityMapper)
        .mapLeft(failureMapper)

suspend inline fun <reified E, reified T> Either<E, T>.send(
    call: RoutingCall,
    status: HttpStatusCode = OK,
) where T : DtoResponse, E : FailureResponse {
    onLeft { call.respond(it.status(), it) }
    onRight { call.respond(status, it) }
}
