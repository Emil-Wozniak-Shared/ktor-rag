package pl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.config.yaml.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.mockk
import kotlinx.serialization.json.Json
import pl.config.DatabaseFactory
import pl.repo.DocumentsRepo
import pl.repo.EmbeddingsRepo
import pl.service.ai.AiAgentService
import pl.service.ai.EmbeddingService
import pl.service.ai.EmbeddingServiceImpl
import pl.service.ai.OpenAiAgentService
import pl.service.ai.RAGService
import pl.service.ai.RAGServiceImpl
import pl.service.cache.RedisService
import pl.service.docs.DocumentService
import pl.service.docs.DocumentServiceImpl

interface IntegrationSpec {
    companion object {
        val documentsRepo: DocumentsRepo = mockk()
        val redisService: RedisService = mockk()
        val embeddingsRepo: EmbeddingsRepo = mockk()
    }

    fun appSpec(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        setup()
        block()
    }

    private fun ApplicationTestBuilder.setup() {
        client = createClient { install(ContentNegotiation) { json() } }
        environment { config = requireNotNull(YamlConfig("application-test.yaml")) }
        application {
            configureMockFrameworks()
            configureRouting()
        }
    }

    private fun Application.configureMockFrameworks() {
        val config = this.environment.config
        dependencies {
            provide<ApplicationConfig> { config }
            provide<DatabaseFactory> { DatabaseFactory(resolve()) }
            provide<HttpClient> {
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
            }
            provide<DocumentsRepo> { documentsRepo }
            provide<EmbeddingsRepo> { embeddingsRepo }
            provide<AiAgentService> { OpenAiAgentService(resolve(), resolve()) }
            provide<RedisService> { redisService }
            provide<EmbeddingService> { EmbeddingServiceImpl(resolve()) }
            provide<DocumentService> { DocumentServiceImpl(resolve(), resolve(), resolve(), resolve(), resolve()) }
            provide<RAGService> { RAGServiceImpl(resolve(), resolve()) }
        }

        val db: DatabaseFactory by dependencies
        db.init()
    }
}