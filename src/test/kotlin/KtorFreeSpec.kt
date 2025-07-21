package pl

import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.yaml.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.mockk
import pl.service.ai.AiAgentService
import pl.service.ai.EmbeddingService
import pl.service.ai.RAGService
import pl.service.cache.RedisService
import pl.service.docs.DocumentService

internal open class KtorFreeSpec(
    body: FreeSpec.() -> Unit = {}
) : FreeSpec({
    body()
}) {
    protected companion object {
        interface Mocks {
            val aiAgentService: AiAgentService
            val redisService: RedisService
            val embeddingService: EmbeddingService
            val documentService: DocumentService
            val ragService: RAGService
        }

        object AppMocks: Mocks {
            override val aiAgentService: AiAgentService = mockk()
            override val redisService: RedisService = mockk()
            override val embeddingService: EmbeddingService = mockk()
            override val documentService: DocumentService = mockk()
            override val ragService: RAGService = mockk()
        }

        fun Application.mockedModule() {
            dependencies {
                provide<AiAgentService> { AppMocks.aiAgentService }
                provide<RedisService> { AppMocks.redisService }
                provide<EmbeddingService> { AppMocks.embeddingService }
                provide<DocumentService> { AppMocks.documentService }
                provide<RAGService> { AppMocks.ragService }
            }
        }

        fun integrationAppSpec(block: suspend ApplicationTestBuilder.(mocks: Mocks) -> Unit) = testApplication {
            client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment { config = requireNotNull(YamlConfig("application.yaml")) }
            application {
                module()
                mockedModule()
            }
            block(AppMocks)
        }
    }
}

