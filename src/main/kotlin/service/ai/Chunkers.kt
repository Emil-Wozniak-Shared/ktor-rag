package pl.service.ai


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


object SemanticChunker {
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