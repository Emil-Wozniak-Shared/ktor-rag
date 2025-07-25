package pl.pl.ejdev.routing.documents.get

import arrow.core.Either
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.mockk.bdd.given
import io.mockk.bdd.then
import pl.pl.ejdev.routing.documents.DocumentApiSpec
import kotlin.test.Test

class GetDocumentsApiSpec : DocumentApiSpec() {
    @Test
    fun `GET api documents works`() = appSpec {
        given { documentService.getAllDocuments() } returns Either.Right(listOf())

        client.get(PATH) shouldHaveStatus OK

        then { documentService.getAllDocuments() }
    }
}