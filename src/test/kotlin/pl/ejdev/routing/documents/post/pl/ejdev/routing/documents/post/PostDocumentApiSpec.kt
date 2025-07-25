package pl.pl.ejdev.routing.documents.post.pl.ejdev.routing.documents.post

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Created
import io.mockk.bdd.coGiven
import io.mockk.bdd.coThen
import pl.model.redis.DocumentRequest
import pl.pl.ejdev.routing.documents.DocumentApiSpec
import kotlin.test.Test

class PostDocumentApiSpec : DocumentApiSpec() {
    @Test
    fun `POST documents works`() = appSpec {
        val request = DocumentRequest(TITLE, TEST_CONTENT)

        coGiven { documentService.addDocument(request) } returns TITLE

        //when
        val response = client.post(PATH) {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus Created
        coThen { documentService.addDocument(request) }
    }
}