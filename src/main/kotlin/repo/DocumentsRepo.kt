package pl.repo

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import pl.config.Documents
import pl.model.redis.DocumentRequest
import java.util.*

class DocumentsRepo {
    fun add(document: DocumentRequest): String {
        val documentId = UUID.randomUUID().toString()
        transaction {
            Documents.insert {
                it[id] = documentId
                it[title] = document.title
                it[content] = document.content
                it[metadata] = document.metadata
            }
        }
        return documentId
    }
}