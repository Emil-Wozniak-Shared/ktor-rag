package pl

import arrow.core.Either
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.FreeSpec
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
import pl.model.ai.RAGResponse
import pl.model.error.InvalidParamsProblemDetails
import pl.model.redis.DocumentRequest
import pl.model.redis.SearchRequest

 class ApplicationSpec : FreeSpec({
    "GET static resource works" - {
        unitAppSpec {
            // expect
            client.get("/") shouldHaveStatus OK
        }
    }
    "GET api documents works" - {
        unitAppSpec { mocks ->
            given { mocks.documentService.getAllDocuments() } returns Either.Right(listOf())

            client.get("/api/documents") shouldHaveStatus OK

            then { mocks.documentService.getAllDocuments() }
        }
    }
    "POST documents works" - {
        unitAppSpec { mocks ->
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
        unitAppSpec { mocks ->
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
        unitAppSpec { mocks ->
            // given
            val request = SearchRequest("testowana fraza", 100)
            coGiven { mocks.documentService.searchDocuments(request.query, request.limit) } returns listOf()

            //when
            val response = client.post("/api/search") {
                headers { contentType(ContentType.Application.Json) }
                setBody(request)
            }

            // then
            response shouldHaveStatus OK
            coThen { mocks.documentService.searchDocuments(request.query, request.limit) }
        }
    }
    "POST rag documents" - {
        unitAppSpec { mocks ->
            // given
            val request = SearchRequest("testowana fraza", 100)
            coGiven { mocks.ragService.generateAnswer(request.query) } returns RAGResponse(request.query, "answer", listOf())

            //when
            val response = client.post("/api/rag") {
                headers { contentType(ContentType.Application.Json) }
                setBody(request)
            }

            // then
            response shouldHaveStatus OK
            coThen { mocks.ragService.generateAnswer(request.query) }
        }
    }
})

