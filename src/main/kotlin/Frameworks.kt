package pl

import io.ktor.client.*
import io.ktor.client.engine.cio.*
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
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

fun jedisPool(config: ApplicationConfig) = JedisPool(
    /* poolConfig = */ JedisPoolConfig(),
    /* host = */ config.property("jedis.host").getString(),
    /* port = */ config.property("jedis.port").getString().toInt(),
    2000,
    /* password = */ config.property("jedis.password").getString(),
)

fun DependencyRegistry.system(app: Application) = this.apply {
    val config = app.environment.config
    provide<ApplicationConfig> { config }
    provide<DatabaseFactory> { DatabaseFactory(resolve()) }
    provide<JedisPool> { jedisPool(resolve()) }
    provide<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
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
}

fun Application.configureFrameworks() {
    dependencies {
        system(this@configureFrameworks)
        services()
    }

    val db: DatabaseFactory by dependencies
    db.init()
}

