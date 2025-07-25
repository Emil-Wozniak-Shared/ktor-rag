package pl.pl.ejdev.routing

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.config.yaml.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.mockk
import kotlinx.serialization.json.Json
import pl.config.DatabaseFactory
import pl.configureRouting
import pl.service.ai.AiAgentService
import pl.service.ai.EmbeddingService
import pl.service.ai.RAGService
import pl.service.cache.RedisService
import pl.service.docs.DocumentService

interface MockedSpec {
    val aiAgentService: AiAgentService
    val redisService: RedisService
    val embeddingService: EmbeddingService
    val documentService: DocumentService
    val ragService: RAGService
}

suspend inline infix fun <reified B: Any> HttpResponse.shouldHaveBody(block: B.() -> Unit) {
    val body = body<B>()
    block(body)
}

abstract class ApiSpec : MockedSpec {

    override val aiAgentService: AiAgentService = mockk()
    override val redisService: RedisService = mockk()
    override val embeddingService: EmbeddingService = mockk()
    override val documentService: DocumentService = mockk()
    override val ragService: RAGService = mockk()

    protected fun appSpec(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
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
            provide<AiAgentService> { aiAgentService }
            provide<RedisService> { redisService }
            provide<EmbeddingService> { embeddingService }
            provide<DocumentService> { documentService }
            provide<RAGService> { ragService }
        }

        val db: DatabaseFactory by dependencies
        db.init()
    }
}