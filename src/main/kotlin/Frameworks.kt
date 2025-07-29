package pl

import FileProcessor
import io.github.domgew.kedis.KedisClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.di.*
import kotlinx.serialization.json.Json
import pl.config.DatabaseFactory
import pl.repo.DocumentsRepo
import pl.repo.EmbeddingsRepo
import pl.service.ai.*
import pl.service.cache.RedisService
import pl.service.cache.RedisServiceImpl
import pl.service.docs.DocumentService
import pl.service.docs.DocumentServiceImpl
import kotlin.time.Duration.Companion.milliseconds


fun kedis(config: ApplicationConfig) = KedisClient.builder {
    // OR: unixSocket
    hostAndPort(
        host = config.property("jedis.host").getString(),
        port = config.property("jedis.port").getString().toInt(), // optional, 6379 is the default
    )
    // OR: noAutoAuth (optional)
    autoAuth(
        password = config.property("jedis.password").getString(),
//        username = "admin", // optional
    )
    connectTimeout = 250.milliseconds
    keepAlive = true // optional, true is the default
    databaseIndex = 1 // optional, 0 is the default
}

private fun DependencyRegistry.system(app: Application) = this.apply {
    val config = app.environment.config
    provide<ApplicationConfig> { config }
    provide<DatabaseFactory> { DatabaseFactory(resolve()) }
    provide<KedisClient> { kedis(resolve()) }
    provide<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout)
        }
    }
}

private fun DependencyRegistry.services() = this.apply {
    provide<DocumentsRepo> { DocumentsRepo() }
    provide<EmbeddingsRepo> { EmbeddingsRepo() }
    provide<AiAgentService> { OpenAiAgentService(resolve(), resolve()) }
    provide<RedisService> { RedisServiceImpl(resolve()) }
    provide<EmbeddingService> { EmbeddingServiceImpl(resolve()) }
    provide<DocumentService> { DocumentServiceImpl(resolve(), resolve(), resolve(), resolve(), resolve()) }
    provide<RAGService> { RAGServiceImpl(resolve(), resolve()) }
    provide<FileProcessor> { FileProcessor(resolve()) }
}

fun Application.configureFrameworks() {
    dependencies {
        system(this@configureFrameworks)
        services()
    }

    val db: DatabaseFactory by dependencies
    db.init()
}

