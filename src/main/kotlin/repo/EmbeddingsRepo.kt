package pl.repo

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import pl.config.Embeddings
import java.util.*

class EmbeddingsRepo {
    fun create(documentId: String, index: Int, chunk: String, embedding: String) {
        val uuid = UUID.randomUUID().toString()
        transaction {
            Embeddings.insert {
                it[id] = uuid
                it[this.documentId] = documentId
                it[this.embedding] = embedding
                it[chunkIndex] = index
                it[chunkContent] = chunk
            }
        }
    }
}