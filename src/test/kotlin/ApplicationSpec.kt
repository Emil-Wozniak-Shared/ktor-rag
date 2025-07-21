package pl

import arrow.core.Either
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.OK
import io.mockk.bdd.coGiven
import io.mockk.bdd.coThen
import io.mockk.bdd.given
import io.mockk.bdd.then
import pl.model.error.InvalidParamsProblemDetails
import pl.model.redis.DocumentRequest
import pl.model.redis.SearchRequest

internal class ApplicationSpec : KtorFreeSpec({
    "GET static resource works" - {
        integrationAppSpec {
            // expect
            client.get("/") shouldHaveStatus OK
        }
    }
    "GET api documents works" - {
        integrationAppSpec { mocks ->
            given { mocks.documentService.getAllDocuments() } returns Either.Right(listOf())

            client.get("/api/documents") shouldHaveStatus OK

            then { mocks.documentService.getAllDocuments() }
        }
    }
    "POST documents works" - {
        integrationAppSpec { mocks ->
            val request = DocumentRequest("Tytuł testowy", "test content")
            coGiven { mocks.documentService.addDocument(request) } returns "Tytuł testowy"

            //when
            val response = client.post("/api/documents") {
                headers { contentType(ContentType.Application.Json) }
                setBody(request)
            }

            // then
            response shouldHaveStatus Created
            coThen { mocks.documentService.addDocument(request) }
        }
    }
    "POST documents fails on short title" - {
        integrationAppSpec { mocks ->
            // given
            val request = DocumentRequest("test", "test content")
            val uri = "/api/documents"

            //when
            val response = client.post(uri) {
                headers { contentType(ContentType.Application.Json) }
                setBody(request)
            }
            // then
            response shouldHaveStatus BadRequest
            response.body<InvalidParamsProblemDetails>().run {
                title shouldBe "Invalid parameters"
                type shouldBe uri
                invalidParams shouldHaveSize 1
                invalidParams[0].name shouldBe "Incorrect param: title"
                invalidParams[0].reason shouldBe " Must have at least 5 characters"
            }

            coThen(exactly = 0) { mocks.documentService.addDocument(any()) }
        }
    }
    "POST search documents" - {
        integrationAppSpec {
            // given
            val request = SearchRequest("testowana fraza", 100)

            //when
            val response = client.post("/api/search") {
                headers { contentType(ContentType.Application.Json) }
                setBody(request)
            }
            // then
            response shouldHaveStatus OK
        }
    }
})

