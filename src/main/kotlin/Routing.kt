package pl

import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.di.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.internal.readJson
import pl.ext.send
import pl.ext.toDto
import pl.model.ai.AiFailureResponse
import pl.model.ai.AskAiResponse
import pl.model.ai.aiFailureMapper
import pl.model.ai.askAiResponseMapper
import pl.model.error.InvalidParamsProblemDetails
import pl.model.redis.*
import pl.service.ai.AiAgentService
import pl.service.ai.RAGService
import pl.service.docs.DocumentService

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause -> call.respondText(text = "500: $cause", status = InternalServerError) }
        exception<RequestValidationException> { call, cause ->
            call.respond(status = BadRequest, message = InvalidParamsProblemDetails(call, cause))
        }
    }
    install(RequestValidation) {
        validate<SearchRequest> { SearchRequestValidator(it).validate() }
        validate<DocumentRequest> { DocumentRequestValidator(it).validate() }
    }
    install(Resources)
    install(ContentNegotiation) {
        json()
    }

    val openAiAgentService: AiAgentService by dependencies
    val documentService: DocumentService by dependencies
    val ragService: RAGService by dependencies

    routing {
        staticResources("/", "static")
        route("/api") {
            get("/chat") {
                openAiAgentService.ask(this.call.queryParameters.q)
                    .toDto(aiFailureMapper, askAiResponseMapper)
                    .send<AiFailureResponse, AskAiResponse>(call)
            }
            get("/documents") {
                documentService.getAllDocuments()
                    .toDto(documentsFailureResponseMapper, documentsMapper)
                    .send(call)
            }
            post("/documents") {
                val documentRequest = call.receive<DocumentRequest>()
                val id = documentService.addDocument(documentRequest)
                call.respond(Created, mapOf("id" to id))
            }
            post("/documents/xwiki") {
                documentService.loadDocumentsFromXWiki()
                call.respond(Created, mapOf("status" to "OK"))
            }
            post("/search") {
                val request = call.receive<SearchRequest>()
                documentService.searchDocuments(request.query, request.limit).let { call.respond(it) }
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

