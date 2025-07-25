package pl.pl.ejdev.routing.rag.post.pl.ejdev.routing.rag.post

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.mockk.bdd.coGiven
import io.mockk.bdd.coThen
import pl.model.ai.RAGResponse
import pl.model.redis.SearchRequest
import pl.pl.ejdev.routing.ApiSpec
import kotlin.test.Test

class PostRagSearchPageSpec : ApiSpec() {

    @Test
    fun `POST rag documents`() = appSpec {
        // given
        val request = SearchRequest(QUERY, 100)
        val ragResponse = RAGResponse(request.query, ANSWER, listOf())

        coGiven { ragService.generateAnswer(request.query) } returns ragResponse

        //when
        val response = client.post("/api/rag") {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus OK
        coThen { ragService.generateAnswer(request.query) }
    }

    companion object {
        private const val QUERY = "testowana fraza"
        private const val ANSWER = "answer"
    }
}