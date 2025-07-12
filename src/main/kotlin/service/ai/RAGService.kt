package pl.service.ai

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.oshai.kotlinlogging.KotlinLogging
import pl.model.ai.AiFailure
import pl.model.ai.RAGResponse
import pl.service.docs.DocumentService

interface RAGService {
    suspend fun generateAnswer(query: String): RAGResponse
}

private val CTX_REGEX = "[.!?]+".toRegex()
private val TEXT_REGEX = "\\W+".toRegex()
private val WHITESPACE = "\\s+".toRegex()

class RAGServiceImpl(
    private val documentService: DocumentService,
    private val aiAgentService: AiAgentService,
) : RAGService {
    private val logger = KotlinLogging.logger {}

    override suspend fun generateAnswer(query: String): RAGResponse =
        logger.info { "Generate answer: $query" }
            .run {
                documentService.searchDocuments(query, 3)
                    .let { searchResults ->
                        RAGResponse(
                            query = query,
                            answer = generateAnswer(query, context = searchResults.joinToString("\n\n") { it.content })
                                .getOrElse { it.message },
                            sources = searchResults
                        )
                    }
            }


    private suspend fun generateAnswer(query: String, context: String): Either<AiFailure, String> {
        val keywords = query.lowercase().split(TEXT_REGEX).filter { it.length > 2 }
        val relevantSentences = context.split(CTX_REGEX)
            .filter { sentence -> keywords.any { keyword -> sentence.lowercase().contains(keyword) } }
            .take(3)

        if (relevantSentences.isNotEmpty()) {
            val input = relevantSentences.joinToString(" ")
                .replace(WHITESPACE, " ")
                .trim()
            return aiAgentService.ask("Based on the available documents, $input")
        } else {
            return either { "I couldn't find specific information about '$query' in the available documents." }
        }
    }
}