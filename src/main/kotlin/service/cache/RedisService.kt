package pl.service.cache

import kotlinx.serialization.json.Json
import pl.model.redis.SearchResult
import redis.clients.jedis.Jedis
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
) : RedisService {
    private val json = Json { ignoreUnknownKeys = true }

    override fun cacheSearchResults(query: String, results: List<SearchResult>): Unit = pool.resource.use { jedis ->
        jedis.expire(Key(query), 1, results)
    }

    override fun getCachedSearchResults(query: String): List<SearchResult> = pool.resource.use { jedis ->
        jedis.get("search:${query.hashCode()}")
            ?.let { json.decodeFromString<List<SearchResult>>(it) }
            .orEmpty()
    }

    override fun cacheEmbedding(text: String, embedding: List<Float>): Unit = pool.resource.use { jedis ->
        jedis.expire(Key(text), hours = 24, embedding)
    }

    override fun getCachedEmbedding(text: String): List<Float> = pool.resource.use { jedis ->
        jedis.get(Key(text).hash())
            ?.let { json.decodeFromString<List<Float>>(it) }
            .orEmpty()
    }

    private inline fun <reified T> Jedis.expire(key: Key, hours: Int, embedding: List<T>) {
        this.setex(key.hash(), hours.hours.inWholeSeconds, embedding.toJson<T>())
    }

    private inline fun <reified T> List<T>.toJson(): String = json.encodeToString(this)

    private data class Key(private val text: String) {
        fun hash() = "embedding:${text.hashCode()}"
    }
}