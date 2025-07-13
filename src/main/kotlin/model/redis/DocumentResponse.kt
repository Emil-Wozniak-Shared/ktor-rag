package pl.model.redis

import kotlinx.serialization.Serializable
import pl.model.DtoResponse

@Serializable
data class DocumentResponse(
    val id: String,
    val title: String,
    val content: String,
    val metadata: String? = null
)

@Serializable
data class DocumentsResponse(
    val documents: List<DocumentResponse>
): DtoResponse

val documentsMapper: (List<Document>) -> DocumentsResponse = { documents ->
    DocumentsResponse(
        documents = documents.map {
            DocumentResponse(
                id = it.id,
                title = it.title,
                content = it.content,
                metadata = it.metadata
            )
        }
    )
}