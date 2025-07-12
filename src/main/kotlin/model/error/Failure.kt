package pl.model.error

import io.ktor.http.HttpStatusCode

interface Failure {
    val message: String
}

interface FailureResponse {
    val message: String
    val status: HttpStatusCode
}