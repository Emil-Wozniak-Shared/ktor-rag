package pl

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.OK
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.justRun
import io.mockk.verifyAll
import pl.IntegrationSpec.Companion.redisService
import pl.model.error.InvalidParamsProblemDetails
import pl.model.redis.DocumentRequest
import pl.model.redis.SearchRequest
import pl.pl.ejdev.routing.shouldHaveBody
import kotlin.test.Test

private const val TITLE = "Tytuł testowy"
private const val TEST_CONTENT = "test content"
private const val SHORT_TITLE = "test"
private const val QUERY = "testowana fraza"
private const val ANSWER = "answer"

class ApplicationIntegrationSpec : IntegrationSpec {
    @Test
    fun `GET static resource works`() = appSpec {
        // expect
        client.get("/") shouldHaveStatus OK
    }

    @Test
    fun `GET api documents works`() = appSpec {
        client.get("/api/documents") shouldHaveStatus OK
    }

    @Test
    fun `POST documents works`() = appSpec {
        val request = DocumentRequest(TITLE, TEST_CONTENT)

        //when
        val response = client.post("/api/documents") {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus Created
    }

    @Test
    fun `POST documents fails on short title`() = appSpec {
        val request = DocumentRequest(SHORT_TITLE, TEST_CONTENT)
        val uri = "/api/documents"

        val response = client.post(uri) {
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
    }

    @Test
    fun `POST search documents`() = appSpec {
        // given
        val request = SearchRequest(QUERY, 100)
        coEvery { redisService.getCachedSearchResults(any()) } returns listOf()
        coJustRun { redisService.cacheSearchResults(any(), any()) }

        //when
        val response = client.post("/api/search") {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus OK
        coVerifyAll {
            redisService.getCachedSearchResults(any())
            redisService.cacheSearchResults(any(), any())
        }
    }

    @Test
    fun `POST rag documents`() = appSpec {
        // given
        val request = SearchRequest(QUERY, 100)
        coEvery { redisService.getCachedSearchResults(any()) } returns listOf()
        coJustRun { redisService.cacheSearchResults(any(), any()) }

        //when
        val response = client.post("/api/rag") {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus OK
        coVerifyAll {
            redisService.getCachedSearchResults(any())
            redisService.cacheSearchResults(any(), any())
        }
    }
}

