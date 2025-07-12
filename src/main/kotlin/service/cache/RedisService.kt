package pl.service.cache

import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import pl.model.redis.SearchResult
import redis.clients.jedis.JedisPool
import kotlin.time.Duration.Companion.hours

interface RedisService {

    fun cacheSearchResults(query: String, results: List<SearchResult>)

    fun getCachedSearchResults(query: String): List<SearchResult>

    fun cacheEmbedding(text: String, embedding: List<Float>)

    fun getCachedEmbedding(text: String): List<Float>
}

class RedisServiceImpl(
    private val pool: JedisPool,
    private val config: ApplicationConfig
): RedisService {
    private val json = Json { ignoreUnknownKeys = true }

    override fun cacheSearchResults(query: String, results: List<SearchResult>) {
        pool.resource.use { jedis ->
            val key = "search:${query.hashCode()}"
            val value = json.encodeToString(results)
            jedis.setex(key, 1.hours.inWholeSeconds, value) // Cache for 1 hour
        }
    }

    override fun getCachedSearchResults(query: String): List<SearchResult> {
        return pool.resource.use { jedis ->
            val key = "search:${query.hashCode()}"
            val value = jedis.get(key)
            if (value != null) {
                json.decodeFromString<List<SearchResult>>(value)
            } else listOf()
        }
    }

    override fun cacheEmbedding(text: String, embedding: List<Float>) {
        pool.resource.use { jedis ->
            val key = "embedding:${text.hashCode()}"
            val value = json.encodeToString(embedding)
            jedis.setex(key, 86400, value) // Cache for 24 hours
        }
    }

    override fun getCachedEmbedding(text: String): List<Float> {
        return pool.resource.use { jedis ->
            val key = "embedding:${text.hashCode()}"
            val value = jedis.get(key)
            if (value != null) {
                json.decodeFromString<List<Float>>(value)
            } else listOf()
        }
    }
}