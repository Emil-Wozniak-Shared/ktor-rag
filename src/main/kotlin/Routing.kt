package pl

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import pl.ext.send
import pl.ext.toDto
import pl.model.ai.AiFailureResponse
import pl.model.ai.AskAiResponse
import pl.model.ai.aiFailureMapper
import pl.model.ai.askAiResponseMapper
import pl.model.redis.DocumentRequest
import pl.model.redis.SearchRequest
import pl.service.ai.AiAgentService
import pl.service.ai.RAGService
import pl.service.docs.DocumentService

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause -> call.respondText(text = "500: $cause", status = InternalServerError) }
    }
    install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello")) ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }
    install(Resources)
    install(ContentNegotiation) {
        json()
    }

    val openAiAgentService: AiAgentService by dependencies
    val documentService: DocumentService by dependencies
    val ragService: RAGService by dependencies

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        staticResources("/static", "static")
        get<Articles> { article -> call.respond("List of articles sorted starting from ${article.sort}") }
        route("/api") {
            get("/chat") {
                openAiAgentService.ask(this.call.queryParameters.q)
                    .toDto(aiFailureMapper, askAiResponseMapper)
                    .send<AiFailureResponse, AskAiResponse>(call)
            }
            post("/documents") {
                call.receive<DocumentRequest>()
                    .let { documentService.addDocument(it) }
                    .let { call.respond(HttpStatusCode.Created, mapOf("id" to it)) }
            }

            get("/documents") {
                documentService.getAllDocuments().let { call.respond(it) }
            }

            post("/search") {
                val request = call.receive<SearchRequest>()
                documentService.searchDocuments(request.query, request.limit)
                    .let { call.respond(it) }
            }
            post("/rag") {
                call.receive<SearchRequest>()
                    .let { (query, _) -> ragService.generateAnswer(query) }
                    .let { call.respond(it) }
            }
        }
    }
}

private val Parameters.q: String
    get() = this["q"]!!

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
