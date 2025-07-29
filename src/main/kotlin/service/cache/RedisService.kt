package pl.service.cache

import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.arguments.value.SetOptions
import io.github.domgew.kedis.commands.KedisValueCommands
import kotlinx.serialization.json.Json
import pl.model.redis.SearchResult
import kotlin.time.Duration.Companion.hours

interface RedisService {
    suspend fun cacheSearchResults(query: String, results: List<SearchResult>)
    suspend fun getCachedSearchResults(query: String): List<SearchResult>
    suspend fun cacheEmbedding(text: String, embedding: List<Float>)
    suspend fun getCachedEmbedding(text: String): List<Float>
}

class RedisServiceImpl(
    private val kedisClient: KedisClient,
) : RedisService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun cacheSearchResults(query: String, results: List<SearchResult>): Unit = kedisClient.use { jedis ->
        jedis.expire(Key(query), 1, results)
    }

    override suspend fun getCachedSearchResults(query: String): List<SearchResult> = kedisClient.use { client ->
        client.execute(KedisValueCommands.get( "search:${query.hashCode()}"))
            ?.let { json.decodeFromString<List<SearchResult>>(it) }
            .orEmpty()
    }

    override suspend fun cacheEmbedding(text: String, embedding: List<Float>): Unit = kedisClient.use { client ->
        client.expire(Key(text), hours = 24, embedding)
    }

    override suspend fun getCachedEmbedding(text: String): List<Float> = kedisClient.use { client ->
        client.execute(KedisValueCommands.get(Key(text).hash()))
            ?.let { json.decodeFromString<List<Float>>(it) }
            .orEmpty()
    }

    private suspend inline fun <reified T> KedisClient.expire(key: Key, hours: Int, embedding: List<T>) {
        this.execute(
            KedisValueCommands.set(
                key = key.hash(),
                value = embedding.toJson(),
                options = SetOptions(
                    expire = SetOptions.ExpireOption.ExpiresAtUnixEpochMillisecond(
                        hours.hours.inWholeSeconds
                    )
                )
            ),
        )
    }

    private inline fun <reified T> List<T>.toJson(): String = json.encodeToString(this)

    private data class Key(private val text: String) {
        fun hash() = "embedding:${text.hashCode()}"
    }
}