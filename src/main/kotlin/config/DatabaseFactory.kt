package pl.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import pl.ext.test
import java.time.LocalDateTime

class DatabaseFactory(
    private val config: ApplicationConfig
) {
    private val logger = KotlinLogging.logger {  }
    fun init() {
        val testing: Boolean = config.test
        val driverClassName = if (!testing) "org.postgresql.Driver" else "org.h2.Driver"
        val url = config.property("database.url").getString()
        logger.info { "Connecting to postgres database at $url" }
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()
        Database.connect(url, driverClassName, user, password)

        transaction {
            SchemaUtils.create(Documents, Embeddings)
        }
    }
}

@Suppress("unused")
object Documents : Table() {
    val id = varchar("id", 50)
    val title = varchar("title", 255)
    val content = text("content")
    val metadata = text("metadata").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id, name = "PK_Documents_Id")
}

object Embeddings : Table() {
    val id = varchar("id", 50)
    val documentId = varchar("document_id", 50).references(Documents.id)
    val embedding = text("embedding") // JSON array of floats
    val chunkIndex = integer("chunk_index")
    val chunkContent = text("chunk_content")
    override val primaryKey = PrimaryKey(id, name = "PK_Embeddings_Id")
}