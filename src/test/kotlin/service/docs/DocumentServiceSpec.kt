package service.docs;

import arrow.core.Either
import arrow.core.right
import io.kotest.common.runBlocking
import io.kotest.matchers.equals.shouldNotBeEqual
import io.ktor.client.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import pl.model.redis.DocumentRequest
import pl.repo.DocumentsRepo
import pl.repo.EmbeddingsRepo
import pl.service.ai.EmbeddingService
import pl.service.cache.RedisService
import pl.service.docs.DocumentService
import pl.service.docs.DocumentServiceImpl
import java.util.*
import kotlin.test.Test

class DocumentServiceSpec {

    private val embeddingService: EmbeddingService = mockk()
    private val redisService: RedisService = mockk()
    private val client: HttpClient = mockk()
    private val documentsRepo: DocumentsRepo = mockk()
    private val embeddingsRepo: EmbeddingsRepo = mockk()

    private val documentService: DocumentService = DocumentServiceImpl(
        embeddingService, redisService, client, documentsRepo, embeddingsRepo
    )

    @Test
    fun `document will be added`() {
        // given:
        every { documentsRepo.add(any()) } returns UUID.randomUUID().toString()
        justRun { embeddingsRepo.create(any(), any(), any(), any()) }
        coEvery { embeddingService.generateEmbedding(any()) } returns listOf<Float>().right()

        val request = DocumentRequest("TITLE", "Some content")

        // when
        val response = runBlocking { documentService.addDocument(request) }

        // then
        response shouldNotBeEqual ""
    }
}
