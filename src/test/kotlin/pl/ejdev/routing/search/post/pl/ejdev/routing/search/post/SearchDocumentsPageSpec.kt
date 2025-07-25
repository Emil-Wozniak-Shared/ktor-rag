package pl.pl.ejdev.routing.search.post.pl.ejdev.routing.search.post

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.OK
import io.mockk.bdd.coGiven
import io.mockk.bdd.coThen
import pl.model.redis.SearchRequest
import pl.pl.ejdev.routing.ApiSpec
import kotlin.test.Test

class SearchDocumentsPageSpec : ApiSpec() {

    @Test
    fun `POST search documents`() = appSpec {
        // given
        val request = SearchRequest(QUERY, 100)
        coGiven { documentService.searchDocuments(request.query, request.limit) } returns listOf()

        //when
        val response = client.post("/api/search") {
            headers { contentType(ContentType.Application.Json) }
            setBody(request)
        }

        // then
        response shouldHaveStatus OK
        coThen { documentService.searchDocuments(request.query, request.limit) }
    }

    companion object {
        const val QUERY = "testowana fraza"
    }
}