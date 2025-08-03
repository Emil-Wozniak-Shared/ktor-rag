package pl.service.ai

import java.io.File


private const val NEW_LINE = "\n"

class SlidingWindowProcessor(
    private val windowSize: Int = 3
) {
    private val recentChunks = mutableListOf<ProcessedChunk>()

    suspend fun processWithContext(
        chunk: String, chunkIndex: Int, aiProcessor: suspend (String, ChunkContext) -> String
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

    private fun createContextFromWindow(): ChunkContext =
        if (recentChunks.isEmpty()) ChunkContext()
        else ChunkContext(
            summary = recentChunks.joinToString(NEW_LINE) { "Chunk ${it.index}: ${it.aiResponse.take(100)}..." },
            entities = recentChunks.flatMap { it.context.entities }.toSet(),
            previousChunkContent = recentChunks.lastOrNull()?.content?.take(200) ?: ""
        )
}

class ContextualFileProcessor(
    private val contextManager: AIContextManager = AIContextManager(),
    private val chunker: SemanticChunker = SemanticChunker
) {

    suspend fun processFile(
        filePath: String, chunkSize: Int = 2000, aiProcessor: suspend (String, ChunkContext) -> String
    ): List<ProcessedChunk> {
        val content = File(filePath).readText()
        val fileName = File(filePath).nameWithoutExtension
        val chunks = chunker.chunkByParagraphs(content, chunkSize)
        var currentContext = contextManager.createInitialContext(fileName, "document")
        val processedChunks = mutableListOf<ProcessedChunk>()
        chunks.forEachIndexed { index, chunk ->
            val contextPrompt = buildContextPrompt(chunk, currentContext, index, chunks.size)
            val aiResponse = aiProcessor(contextPrompt, currentContext)
            val processedChunk = ProcessedChunk(content = chunk, index, context = currentContext, aiResponse)
            processedChunks.add(processedChunk)
            currentContext = contextManager.updateContext(
                previousContext = currentContext,
                currentChunk = chunk,
                aiResponse = aiResponse,
                chunkIndex = index
            )
        }

        return processedChunks
    }

    private fun buildContextPrompt(
        chunk: String, context: ChunkContext, chunkIndex: Int, totalChunks: Int
    ): String = buildString {
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
