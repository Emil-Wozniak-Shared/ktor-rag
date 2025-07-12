package pl.model.ai

import kotlinx.serialization.Serializable
import pl.model.DtoResponse
import pl.model.redis.SearchResult

@Serializable
data class RAGResponse(
    val query: String,
    val answer: String,
    val sources: List<SearchResult>
): DtoResponse
