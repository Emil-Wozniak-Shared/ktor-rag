@file:Suppress("PropertyName")

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.runBlocking
import pl.service.ai.AiAgentService
import java.io.File

object ITextPDFExtractor {

    fun extractTextFromPDF(filePath: String): String {
        return try {
            val reader = PdfReader(File(filePath))
            val pdfDocument = PdfDocument(reader)
            val text = StringBuilder()

            for (i in 1..pdfDocument.numberOfPages) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
            }

            pdfDocument.close()
            text.toString()
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }

    fun extractTextFromPage(filePath: String, pageNumber: Int): String {
        return try {
            val reader = PdfReader(File(filePath))
            val pdfDocument = PdfDocument(reader)

            if (pageNumber > pdfDocument.numberOfPages) {
                println("Page number exceeds total pages")
                return ""
            }

            val text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNumber))
            pdfDocument.close()
            text
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }

    fun extractTextFromBytes(pdfBytes: ByteArray): String {
        return try {
            val reader = PdfReader(pdfBytes.inputStream())
            val pdfDocument = PdfDocument(reader)
            val text = StringBuilder()

            for (i in 1..pdfDocument.numberOfPages) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)))
            }

            pdfDocument.close()
            text.toString()
        } catch (e: Exception) {
            println("Error reading PDF: ${e.message}")
            ""
        }
    }
}

// Data classes for context management
data class ChunkContext(
    val summary: String = "",
    val entities: Set<String> = emptySet(),
    val themes: List<String> = emptyList(),
    val previousChunkContent: String = "",
    val metadata: Map<String, Any> = emptyMap()
)

data class ProcessedChunk(
    val content: String, val index: Int, val context: ChunkContext, val aiResponse: String = ""
)

// 1. OVERLAP STRATEGY - Create overlapping chunks
class OverlapChunker(private val overlapPercentage: Double = 0.15) {

    fun chunkWithOverlap(text: String, chunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        val overlapSize = (chunkSize * overlapPercentage).toInt()
        var start = 0

        while (start < text.length) {
            val end = minOf(start + chunkSize, text.length)
            val chunk = text.substring(start, end)
            chunks.add(chunk)

            // Move start position accounting for overlap
            start = if (end == text.length) text.length else end - overlapSize
        }

        return chunks
    }
}

// 2. SEMANTIC CHUNKING - Split at natural boundaries
class SemanticChunker {

    fun chunkByParagraphs(text: String, maxChunkSize: Int): List<String> {
        val paragraphs = text.split("\n\n")
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        paragraphs.forEach { paragraph ->
            if (currentChunk.length + paragraph.length > maxChunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }
            currentChunk.append(paragraph).append("\n\n")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }

    fun chunkBySentences(text: String, maxChunkSize: Int): List<String> {
        val sentences = text.split(". ")
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            val sentenceWithPeriod = if (sentence.endsWith(".")) sentence else "$sentence."

            if (currentChunk.length + sentenceWithPeriod.length > maxChunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }
            currentChunk.append(sentenceWithPeriod).append(" ")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks
    }
}

// 3. CONTEXT MANAGER - Maintains running context
class AIContextManager {

    fun createInitialContext(documentTitle: String, documentType: String): ChunkContext = ChunkContext(
        metadata = mapOf(
            "documentTitle" to documentTitle,
            "documentType" to documentType,
            "startTime" to System.currentTimeMillis()
        )
    )

    fun updateContext(
        previousContext: ChunkContext,
        currentChunk: String,
        aiResponse: String,
        chunkIndex: Int
    ): ChunkContext {
        // Extract entities (simplified - you'd use NLP library in practice)
        val newEntities = extractEntities(currentChunk)

        // Generate summary of the current chunk
        val chunkSummary = generateSummary(currentChunk, aiResponse)

        // Update running summary
        val updatedSummary = if (previousContext.summary.isEmpty()) {
            chunkSummary
        } else {
            "${previousContext.summary}\n\nChunk $chunkIndex: $chunkSummary"
        }

        return previousContext.copy(
            summary = updatedSummary,
            entities = previousContext.entities + newEntities,
            previousChunkContent = currentChunk.take(200) + "...", // Keep snippet of previous chunk
            metadata = previousContext.metadata + ("lastProcessedChunk" to chunkIndex)
        )
    }

    private fun extractEntities(text: String): Set<String> {
        // Simplified entity extraction - in practice use libraries like CoreNLP or spaCy
        val words = text.split(TEXT_REGEX)
        return words.filter { it.length > 3 && it[0].isUpperCase() }.toSet()
    }

    private fun generateSummary(chunk: String, aiResponse: String): String {
        // Simplified summary - you'd use AI for this
        val sentences = chunk.split(". ")
        return sentences.take(2).joinToString(". ")
    }
}

private val TEXT_REGEX = "\\s+".toRegex()

// 4. SLIDING WINDOW PROCESSOR - Maintains memory buffer
class SlidingWindowProcessor(private val windowSize: Int = 3) {
    private val recentChunks = mutableListOf<ProcessedChunk>()

    fun processWithContext(
        chunk: String, chunkIndex: Int, aiProcessor: (String, ChunkContext) -> String
    ): ProcessedChunk {
        // Create context from recent chunks
        val context = createContextFromWindow()

        // Process current chunk with AI
        val aiResponse = aiProcessor(chunk, context)

        // Create processed chunk
        val processedChunk = ProcessedChunk(
            content = chunk, index = chunkIndex, context = context, aiResponse = aiResponse
        )

        // Add to sliding window
        recentChunks.add(processedChunk)
        if (recentChunks.size > windowSize) {
            recentChunks.removeAt(0)
        }

        return processedChunk
    }

    private fun createContextFromWindow(): ChunkContext {
        if (recentChunks.isEmpty()) return ChunkContext()

        val combinedSummary = recentChunks.joinToString("\n") {
            "Chunk ${it.index}: ${it.aiResponse.take(100)}..."
        }

        val allEntities = recentChunks.flatMap { it.context.entities }.toSet()

        return ChunkContext(
            summary = combinedSummary,
            entities = allEntities,
            previousChunkContent = recentChunks.lastOrNull()?.content?.take(200) ?: ""
        )
    }
}

// 5. COMPLETE FILE PROCESSOR - Puts it all together
class ContextualFileProcessor(
    private val contextManager: AIContextManager = AIContextManager(),
    private val chunker: SemanticChunker = SemanticChunker()
) {

    suspend fun processFile(
        filePath: String, chunkSize: Int = 2000, aiProcessor: suspend (String, ChunkContext) -> String
    ): List<ProcessedChunk> {
        // Read file
        val content = File(filePath).readText()
        val fileName = File(filePath).nameWithoutExtension

        // Create chunks
        val chunks = chunker.chunkByParagraphs(content, chunkSize)

        // Initialize context
        var currentContext = contextManager.createInitialContext(fileName, "document")
        val processedChunks = mutableListOf<ProcessedChunk>()

        // Process each chunk with accumulated context
        chunks.forEachIndexed { index, chunk ->
            // Create context prompt for AI
            val contextPrompt = buildContextPrompt(chunk, currentContext, index, chunks.size)

            // Process with AI
            val aiResponse = aiProcessor(contextPrompt, currentContext)

            // Create processed chunk
            val processedChunk = ProcessedChunk(
                content = chunk, index = index, context = currentContext, aiResponse = aiResponse
            )
            processedChunks.add(processedChunk)

            // Update context for next chunk
            currentContext = contextManager.updateContext(
                currentContext, chunk, aiResponse, index
            )
        }

        return processedChunks
    }

    private fun buildContextPrompt(
        chunk: String, context: ChunkContext, chunkIndex: Int, totalChunks: Int
    ): String {
        return buildString {
            appendLine("CONTEXT INFORMATION:")
            appendLine("Document: ${context.metadata["documentTitle"]}")
            appendLine("Processing chunk $chunkIndex of $totalChunks")

            if (context.summary.isNotEmpty()) {
                appendLine("\nPREVIOUS CONTEXT:")
                appendLine(context.summary)
            }

            if (context.entities.isNotEmpty()) {
                appendLine("\nKEY ENTITIES MENTIONED:")
                appendLine(context.entities.joinToString(", "))
            }

            if (context.previousChunkContent.isNotEmpty()) {
                appendLine("\nPREVIOUS CHUNK EXCERPT:")
                appendLine(context.previousChunkContent)
            }

            appendLine("\nCURRENT CHUNK TO PROCESS:")
            appendLine(chunk)

            appendLine("\nPlease process this chunk while maintaining awareness of the previous context.")
        }
    }
}

// OpenAI API Data Classes
@kotlinx.serialization.Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val stream: Boolean = false
)

@kotlinx.serialization.Serializable
data class OpenAIMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@kotlinx.serialization.Serializable
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null
)

@kotlinx.serialization.Serializable
data class OpenAIChoice(
    val index: Int, val message: OpenAIMessage, val finish_reason: String?
)

@kotlinx.serialization.Serializable
data class OpenAIUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// AI Processor Factory
internal class AIProcessorFactory(
    private val aiAgentService: AiAgentService
) {

    fun createSummarizationProcessor(): suspend (String, ChunkContext) -> String = { prompt, context ->
        val systemMessage = """
            You are an expert document analyzer. Your task is to process document chunks while maintaining context.
            
            For each chunk you process:
            1. Summarize the key points
            2. Identify important entities, concepts, or themes
            3. Note any connections to previous context
            4. Maintain consistency with the overall document narrative
            
            Be concise but comprehensive in your analysis.
        """.trimIndent()

        aiAgentService.processText(
            prompt = prompt,
            systemMessage = systemMessage,
            model = "gpt-4o-mini",
            maxTokens = 500,
            temperature = 0.3
        ).getOrElse { error ->
            error("Error processing chunk: ${error.message}")
        }
    }

    fun createQuestionAnsweringProcessor(question: String): suspend (String, ChunkContext) -> String =
        { prompt, context ->
            val systemMessage = """
            You are answering the question: "$question"
            
            Use the provided document chunks and context to build a comprehensive answer.
            If the current chunk doesn't contain relevant information, note that but maintain context for future chunks.
            Build upon previous chunks' information when relevant.
            
            Be factual and cite which parts of the text support your answer.
        """.trimIndent()

            aiAgentService.processText(
                prompt = prompt, systemMessage = systemMessage, model = "gpt-4o-mini", maxTokens = 800, temperature = 0.2
            ).getOrElse { error ->
                "Error processing chunk: ${error.message}"
            }
        }

    fun createAnalysisProcessor(analysisType: String): suspend (String, ChunkContext) -> String = { prompt, context ->
        val systemMessage = """
            You are performing $analysisType analysis on a document.
            
            Process each chunk systematically:
            1. Extract relevant information for $analysisType
            2. Consider context from previous chunks
            3. Build comprehensive understanding across chunks
            4. Maintain analytical consistency
            
            Focus on $analysisType while preserving context continuity.
        """.trimIndent()

        aiAgentService.processText(
            prompt = prompt, systemMessage = systemMessage, model = "gpt-4o-mini", maxTokens = 600, temperature = 0.4
        ).getOrElse { error ->
            "Error processing chunk: ${error.message}"
        }
    }
}

// Enhanced Usage Examples with OpenAI
internal class OpenAIExamples(
    private val aiAgentService: AiAgentService

) {
    private val processorFactory = AIProcessorFactory(aiAgentService)

    suspend fun processDocumentWithSummarization(filePath: String) {
        val processor = ContextualFileProcessor()
        val aiProcessor = processorFactory.createSummarizationProcessor()

        try {
            val results = processor.processFile(
                filePath = filePath, chunkSize = 2000, aiProcessor = aiProcessor
            )

            println("=== DOCUMENT PROCESSING RESULTS ===")
            results.forEach { chunk ->
                println("\n--- Chunk ${chunk.index + 1} ---")
                println("Content preview: ${chunk.content.take(100)}...")
                println("AI Analysis:")
                println(chunk.aiResponse)
                println("Entities found: ${chunk.context.entities.joinToString(", ")}")
            }

            // Generate final summary
            val finalSummary = generateFinalSummary(results)
            println("\n=== FINAL DOCUMENT SUMMARY ===")
            println(finalSummary)

        } catch (e: Exception) {
            println("Error processing document: ${e.message}")
        }
    }

    suspend fun answerQuestionFromDocument(filePath: String, question: String) {
        val processor = ContextualFileProcessor()
        val aiProcessor = processorFactory.createQuestionAnsweringProcessor(question)

        try {
            val results = processor.processFile(
                filePath = filePath, chunkSize = 1500, aiProcessor = aiProcessor
            )

            println("=== QUESTION: $question ===")

            val relevantChunks = results.filter {
                it.aiResponse.contains(
                    "relevant", ignoreCase = true
                ) || it.aiResponse.length > 50 // Simple relevance check
            }

            if (relevantChunks.isNotEmpty()) {
                println("\n=== ANSWER BASED ON DOCUMENT ANALYSIS ===")
                relevantChunks.forEach { chunk ->
                    println("\nFrom chunk ${chunk.index + 1}:")
                    println(chunk.aiResponse)
                }

                // Generate comprehensive answer
                val comprehensiveAnswer = generateComprehensiveAnswer(question, relevantChunks)
                println("\n=== COMPREHENSIVE ANSWER ===")
                println(comprehensiveAnswer)
            } else {
                println("No relevant information found in the document for this question.")
            }

        } catch (e: Exception) {
            println("Error answering question: ${e.message}")
        }
    }

    fun performThematicAnalysis(filePath: String, analysisType: String = "thematic") {
        val processor = SlidingWindowProcessor(windowSize = 2)
        processorFactory.createAnalysisProcessor(analysisType)

        // Read and chunk the file
        val content = File(filePath).readText()
        val chunker = SemanticChunker()
        val chunks = chunker.chunkByParagraphs(content, 1800)

        try {
            val results = mutableListOf<ProcessedChunk>()

            for ((index, chunk) in chunks.withIndex()) {
                val result = processor.processWithContext(chunk, index) { chunkContent, context ->
                    val contextPrompt = buildAnalysisPrompt(chunkContent, context, analysisType)

                    runBlocking {
                        aiAgentService.processText(
                            prompt = contextPrompt,
                            systemMessage = "You are performing $analysisType analysis. Focus on themes, patterns, and insights.",
                            model = "gpt-4o-mini",
                            maxTokens = 600,
                            temperature = 0.4
                        ).getOrElse { error ->
                            "Error in analysis: ${error.message}"
                        }
                    }
                }
                results.add(result)

                println("Processed chunk ${index + 1}/${chunks.size}")
            }

            println("\n=== $analysisType ANALYSIS RESULTS ===")
            results.forEach { chunk ->
                println("\n--- Analysis for Chunk ${chunk.index + 1} ---")
                println(chunk.aiResponse)
            }

        } catch (e: Exception) {
            println("Error in thematic analysis: ${e.message}")
        }
    }

    private suspend fun generateFinalSummary(results: List<ProcessedChunk>): String {
        val allSummaries = results.joinToString("\n\n") {
            "Chunk ${it.index + 1}: ${it.aiResponse}"
        }

        return aiAgentService.processText(
            prompt = "Based on these chunk summaries, provide a comprehensive final summary of the entire document:\n\n$allSummaries",
            systemMessage = "You are creating a final document summary. Synthesize all chunk summaries into a coherent overview.",
            model = "gpt-4o-mini",
            maxTokens = 800,
            temperature = 0.3
        ).getOrElse { "Unable to generate final summary: ${it.message}" }
    }

    private suspend fun generateComprehensiveAnswer(question: String, relevantChunks: List<ProcessedChunk>): String {
        val combinedResponses = relevantChunks.joinToString("\n\n") {
            "From chunk ${it.index + 1}: ${it.aiResponse}"
        }

        return aiAgentService.processText(
            prompt = "Question: $question\n\nBased on these chunk analyses, provide a comprehensive answer:\n\n$combinedResponses",
            systemMessage = "Synthesize the information from multiple document chunks to provide a complete answer to the user's question.",
            model = "gpt-4o-mini",
            maxTokens = 1000,
            temperature = 0.2
        ).getOrElse { "Unable to generate comprehensive answer: ${it.message}" }
    }

    private fun buildAnalysisPrompt(chunk: String, context: ChunkContext, analysisType: String): String = buildString {
        appendLine("ANALYSIS TYPE: $analysisType")

        if (context.summary.isNotEmpty()) {
            appendLine("\nPREVIOUS ANALYSIS CONTEXT:")
            appendLine(context.summary)
        }

        appendLine("\nCURRENT CHUNK FOR $analysisType ANALYSIS:")
        appendLine(chunk)

        appendLine("\nPlease analyze this chunk for $analysisType elements while considering the previous context.")
    }
}

internal class FileProcessor(
    private val aiAgentService: AiAgentService

) {
    private val examples = OpenAIExamples(aiAgentService)

    fun resource(name: String = "abstract.pdf") = this::class.java.getResource(name)!!

    suspend fun run() {
        try {
            val textFromPDF = ITextPDFExtractor.extractTextFromPDF(resource().path)
            val filePath = resource("abstract.txt").path
            File(filePath).writeText(textFromPDF)
            // Example 1: Document summarization
            examples.processDocumentWithSummarization(filePath)

            // Example 2: Question answering
            examples.answerQuestionFromDocument(filePath, "What are the main conclusions of this document?")

            // Example 3: Thematic analysis
            examples.performThematicAnalysis(filePath, "sentiment")

        } finally {
        }
    }

}

