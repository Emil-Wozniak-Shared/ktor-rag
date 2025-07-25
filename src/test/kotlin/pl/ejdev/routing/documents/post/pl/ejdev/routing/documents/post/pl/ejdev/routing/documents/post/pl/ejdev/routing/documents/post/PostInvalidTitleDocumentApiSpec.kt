package pl.pl.ejdev.routing.documents.post.pl.ejdev.routing.documents.post.pl.ejdev.routing.documents.post.pl.ejdev.routing.documents.post

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.mockk.bdd.coThen
import pl.model.error.InvalidParamsProblemDetails
import pl.model.redis.DocumentRequest
import pl.pl.ejdev.routing.documents.DocumentApiSpec
import pl.pl.ejdev.routing.shouldHaveBody
import kotlin.test.Test

class PostInvalidTitleDocumentApiSpec : DocumentApiSpec() {
    @Test
    fun `POST documents fails on short title`() = appSpec {
        val request = DocumentRequest(SHORT_TITLE, TEST_CONTENT)
        val uri = "/api/documents"

        val response = client.post(PATH) {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus BadRequest
        response.shouldHaveBody<InvalidParamsProblemDetails> {
            title shouldBe "Invalid parameters"
            type shouldBe uri
            invalidParams shouldHaveSize 1
            invalidParams[0].name shouldBe "Incorrect param: title"
            invalidParams[0].reason shouldBe " Must have at least 5 characters"
        }

        coThen(exactly = 0) { documentService.addDocument(any()) }
    }
}