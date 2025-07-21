package pl.model.redis

import kotlinx.serialization.Serializable
import pl.model.DtoResponse

@Serializable
data class Document(
    val id: String,
    val title: String,
    val content: String,
    val metadata: String? = null
)

@Serializable
data class DocumentRequest(
    val title: String = "",
    val content: String = "",
    val metadata: String? = null
)

@Serializable
data class SearchRequest(
    val query: String,
    val limit: Int
)

@Serializable
data class SearchResult(
    val documentId: String,
    val title: String,
    val content: String,
    val score: Double,
    val chunkIndex: Int
)

@Serializable
data class SearchResponse(
    val documentId: String,
    val title: String,
    val content: String,
    val score: Double,
    val chunkIndex: Int
)

@Serializable
data class SearchResponses(val elements: List<SearchResponse>) : DtoResponse

val searchResponseMapper: (searchResults: List<SearchResult>) -> SearchResponses = {
    SearchResponses(
        elements = it.map { result ->
            SearchResponse(
                documentId = result.documentId,
                title = result.title,
                content = result.content,
                score = result.score,
                chunkIndex = result.chunkIndex
            )
        }
    )
}
