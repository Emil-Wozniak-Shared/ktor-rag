@file:Suppress("PropertyName")

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import pl.model.ai.AIModel
import pl.service.ai.*
import java.io.File

private const val RELEVANT = "relevant"

internal class OpenAIFileProcessorEngines(
    private val aiAgentService: AiAgentService
) {
    private val logger = KotlinLogging.logger {}
    private val processorFactory = AIProcessorFactory(aiAgentService)

    suspend fun processDocumentWithSummarization(filePath: String) = try {
        ContextualFileProcessor()
            .processFile(filePath, chunkSize = 2000, processorFactory.createSummarizationProcessor())
            .also {
                logger.info { """
                    === DOCUMENT PROCESSING RESULTS ===
                    ${it.joinToString("\n") { chunk ->
                    """
                    --- Chunk ${chunk.index + 1} ---
                    Content preview: ${chunk.content.take(100)}...
                    AI Analysis:
                    ${chunk.aiResponse}
                    Entities found: ${chunk.context.entities.joinToString(", ")}
                    """.trimIndent()
                }
                    }
                    """.trimIndent() }
            }
            .let { generateFinalSummary(it) }
            .also { logger.info { "\n=== FINAL DOCUMENT SUMMARY ===\n$it" } }
    } catch (e: Exception) {
        logger.error { "Error processing document: ${e.message}" }
    }

    suspend fun answerQuestionFromDocument(filePath: String, question: String) =
        ContextualFileProcessor()
            .runCatching {
                processFile(
                    filePath = filePath,
                    chunkSize = 1500,
                    aiProcessor = processorFactory.createQuestionAnsweringProcessor(question)
                )
                    .also { logger.info { "=== QUESTION: $question ===" } }
                    .filter { it.relevant }
                    .takeIf(List<ProcessedChunk>::isNotEmpty)
                    ?.log("=== ANSWER BASED ON DOCUMENT ANALYSIS ===") {
                        """
                          === COMPREHENSIVE ANSWER ===
                            ${generateComprehensiveAnswer(question, this)}
                    """.trimIndent()
                    }
                    ?: run { logger.warn { "No relevant information found in the document for this question." } }
            }
            .onFailure { logger.error { "Error answering question: ${it.message}" } }

    /* Simple relevance check */
    private val ProcessedChunk.relevant: Boolean
        get() = aiResponse.contains(RELEVANT, ignoreCase = true) || aiResponse.length > 50

    suspend fun performThematicAnalysis(filePath: String, analysisType: String = "thematic") {
        val processor = SlidingWindowProcessor(windowSize = 2)
        processorFactory.createAnalysisProcessor(analysisType)
        SemanticChunker.chunkByParagraphs(text = File(filePath).readText(), maxChunkSize = 1800)
            .runCatching {
                mapIndexed { index, chunk ->
                    processor
                        .processWithContext(chunk, index) { chunkContent, context ->
                            aiAgentService
                                .processText(
                                    prompt = prepareAnalysisPrompt(chunkContent, context, analysisType),
                                    systemMessage = "You are performing $analysisType analysis. Focus on themes, patterns, and insights.",
                                    model = AIModel.GPT_4O_MINI.value,
                                    maxTokens = 600,
                                    temperature = 0.4
                                )
                                .getOrElse { "Error in analysis: ${it.message}" }
                        }.also { logger.info { "Processed chunk ${index + 1}/${size}" } }
                }.log("=== $analysisType ANALYSIS RESULTS ===")
            }
            .onFailure { logger.error { "Error in thematic analysis: ${it.message}" } }
    }

    private fun List<ProcessedChunk>.log(title: String, ending: List<ProcessedChunk>.() -> String = {""}) = apply {
        logger.info {
            """$title
                 ${this.joinToString("\n") { chunk -> "From chunk ${chunk.index + 1}:${chunk.aiResponse}" }}
                 ${ending()}
            """.trimIndent()
        }
    }

    private suspend fun generateFinalSummary(results: List<ProcessedChunk>): String = aiAgentService.processText(
        prompt = "Based on these chunk summaries, provide a comprehensive final summary of the entire document:\n\n${
            results.joinToString("\n\n") { "Chunk ${it.index + 1}: ${it.aiResponse}" }
        }",
        systemMessage = "You are creating a final document summary. Synthesize all chunk summaries into a coherent overview.",
        model = AIModel.GPT_4O_MINI.value,
        maxTokens = 800,
        temperature = 0.3
    ).getOrElse { "Unable to generate final summary: ${it.message}" }

    private fun generateComprehensiveAnswer(question: String, relevantChunks: List<ProcessedChunk>): String =
        runBlocking {
            aiAgentService.processText(
                prompt = "Question: $question\n\nBased on these chunk analyses, provide a comprehensive answer:\n\n${
                    relevantChunks.joinToString("\n\n") { "From chunk ${it.index + 1}: ${it.aiResponse}" }
                }",
                systemMessage = "Synthesize the information from multiple document chunks to provide a complete answer to the user's question.",
                model = AIModel.GPT_4O_MINI.value,
                maxTokens = 1000,
                temperature = 0.2
            ).getOrElse { "Unable to generate comprehensive answer: ${it.message}" }
        }

    private fun prepareAnalysisPrompt(chunk: String, context: ChunkContext, analysisType: String): String =
        """
        ANALYSIS TYPE: $analysisType
            ${
            if (context.summary.isNotEmpty()) "\nPREVIOUS ANALYSIS CONTEXT:\n${context.summary}"
            else ""
        }
        
        PREVIOUS ANALYSIS CONTEXT:
        ${context.summary}
        
        CURRENT CHUNK FOR $analysisType ANALYSIS:
        $chunk
        
        Please analyze this chunk for $analysisType elements while considering the previous context.
        """.trimIndent()
}

internal class FileProcessor(
    private val aiAgentService: AiAgentService
) {
    private val engines = OpenAIFileProcessorEngines(aiAgentService)

    private fun resource(name: String = "abstract.pdf") = this::class.java.getResource(name)!!

    suspend fun run() = run {
        val filePath = resource("abstract.txt").path
        ITextPDFExtractor
            .runCatching { extractTextFromPDF(resource().path) }
            .onSuccess { File(filePath).writeText(it) }
            .onSuccess {
                engines.processDocumentWithSummarization(filePath)
                engines.answerQuestionFromDocument(filePath, "What are the main conclusions of this document?")
                engines.performThematicAnalysis(filePath, "sentiment")
            }
    }
}

