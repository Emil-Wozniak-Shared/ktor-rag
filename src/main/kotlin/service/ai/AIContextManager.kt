package pl.service.ai

private val TEXT_REGEX = "\\s+".toRegex()

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
        val chunkSummary = generateSummary(currentChunk, aiResponse)
        return previousContext.copy(
            summary =
                if (previousContext.summary.isEmpty()) chunkSummary
                else "${previousContext.summary}\n\nChunk $chunkIndex: $chunkSummary",
            entities = previousContext.entities + extractEntities(currentChunk),
            previousChunkContent = currentChunk.take(200) + "...", // Keep snippet of previous chunk
            metadata = previousContext.metadata + ("lastProcessedChunk" to chunkIndex)
        )
    }

    private fun extractEntities(text: String): Set<String> =
        text.split(TEXT_REGEX).filter { it.length > 3 && it[0].isUpperCase() }.toSet()

    private fun generateSummary(chunk: String, aiResponse: String): String =
        chunk.split(". ").take(2).joinToString(". ")
}