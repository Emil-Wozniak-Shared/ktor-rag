package pl.service.docs

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import pl.config.Documents
import pl.config.Embeddings
import pl.model.redis.Document
import pl.model.redis.DocumentRequest
import pl.model.redis.SearchResult
import pl.service.ai.EmbeddingService
import pl.service.cache.RedisService
import java.util.*

private val regex = "[.!?]+".toRegex()

interface DocumentService {
    suspend fun addDocument(request: DocumentRequest): String

    suspend fun searchDocuments(query: String, limit: Int): List<SearchResult>

    fun getAllDocuments(): List<Document>

    fun chunkText(text: String, maxChunkSize: Int = 500): List<String> {
        val sentences = text.split(regex).filter { it.trim().isNotEmpty() }
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (currentChunk.length + sentence.length > maxChunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }
            currentChunk.append(sentence.trim()).append(". ")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }
}

class DocumentServiceImpl(
    private val embeddingService: EmbeddingService,
    private val redisService: RedisService,
) : DocumentService {
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = KotlinLogging.logger {}

    override suspend fun addDocument(request: DocumentRequest): String {
        logger.info { "Add document: $request" }
        val documentId = UUID.randomUUID().toString()
        val chunks = chunkText(request.content)

        transaction {
            Documents.insert {
                it[id] = documentId
                it[title] = request.title
                it[content] = request.content
                it[metadata] = request.metadata
            }
        }

        // Generate embeddings for each chunk
        chunks.forEachIndexed { index, chunk ->
            val embedding = embeddingService.generateEmbedding(chunk).getOrElse {
                error(it.message)
            }

            transaction {
                Embeddings.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[this.documentId] = documentId
                    it[this.embedding] = json.encodeToString(embedding)
                    it[chunkIndex] = index
                    it[chunkContent] = chunk
                }
            }
        }

        return documentId
    }

    override suspend fun searchDocuments(query: String, limit: Int): List<SearchResult> {
        logger.info { "Search for documents" }
        redisService.getCachedSearchResults(query)
            .takeIf { it.isNotEmpty() }
            ?.let {
                logger.info { "Found documents in cache ${it.size}" }
                return it
            }

        val queryEmbedding = embeddingService.generateEmbedding(query).getOrElse { listOf() }
        val results = mutableListOf<SearchResult>()

        transaction {
            val embeddings = Embeddings.selectAll().map { row ->
                EmbeddingWrapper.from(row)
            }

            val similarities = embeddings.map { (docId, chunkIdx, embedding) ->
                val similarity = embeddingService.cosineSimilarity(
                    queryEmbedding,
                    json.decodeFromString<List<Float>>(embedding)
                )
                Triple(docId, chunkIdx, similarity)
            }.sortedByDescending { it.third }

            val topResults = similarities.take(limit)

            topResults.forEach { (docId, chunkIdx, similarity) ->
                val document = Documents.select { Documents.id eq docId }.single()
                val embeddingRow = Embeddings.select {
                    (Embeddings.documentId eq docId) and (Embeddings.chunkIndex eq chunkIdx)
                }.single()

                results.add(
                    SearchResult(
                        documentId = docId,
                        title = document[Documents.title],
                        content = embeddingRow[Embeddings.chunkContent],
                        score = similarity,
                        chunkIndex = chunkIdx
                    )
                )
            }
        }

        logger.info { "Add documents to cache ${results.size}" }
        redisService.cacheSearchResults(query, results)
        return results
    }

    override fun getAllDocuments(): List<Document> = transaction {
        logger.info { "Get all documents" }
        Documents.selectAll()
            .map { row ->
                Document(
                    id = row[Documents.id],
                    title = row[Documents.title],
                    content = row[Documents.content],
                    metadata = row[Documents.metadata]
                )
            }
    }

    private data class EmbeddingWrapper(
        val documentId: String,
        val chunkIndex: Int,
        val embedding: String
    ) {
        companion object {
            fun from(row: ResultRow) = EmbeddingWrapper(
                row[Embeddings.documentId],
                row[Embeddings.chunkIndex],
                row[Embeddings.embedding]
            )
        }
    }

    private data class Similarity(
        val docId: String,
        val chunkIdx: Int,
        val similarity: Double,
    )
}

