package pl.service.ai

import pl.model.ai.AIModel

suspend fun AiAgentService.ask(
    prompt: String,
    systemMessage: String?,
    model: AIModel = AIModel.GPT_4O_MINI,
    maxTokens: Int? = 500,
    temperature: Double? = 0.3
) = this.processText(
    prompt = prompt,
    systemMessage = systemMessage,
    model = model.value,
    maxTokens = maxTokens,
    temperature = temperature
)

internal class AIProcessorFactory(
    private val aiAgentService: AiAgentService
) {

    fun createSummarizationProcessor(): suspend (String, ChunkContext) -> String = { prompt, context ->
        aiAgentService.ask(
            prompt, """
            You are an expert document analyzer. Your task is to process document chunks while maintaining context.
            
            For each chunk you process:
            1. Summarize the key points
            2. Identify important entities, concepts, or themes
            3. Note any connections to previous context
            4. Maintain consistency with the overall document narrative
            
            Be concise but comprehensive in your analysis.
        """.trimIndent()
        ).getOrElse { error -> error("Error processing chunk: ${error.message}") }
    }

    fun createQuestionAnsweringProcessor(question: String): suspend (String, ChunkContext) -> String =
        { prompt, context ->
            aiAgentService.ask(
                prompt = prompt,
                systemMessage = """
            You are answering the question: "$question"
            
            Use the provided document chunks and context to build a comprehensive answer.
            If the current chunk doesn't contain relevant information, note that but maintain context for future chunks.
            Build upon previous chunks' information when relevant.
            
            Be factual and cite which parts of the text support your answer.
        """.trimIndent(),
                maxTokens = 800,
                temperature = 0.2
            ).getOrElse { error ->
                "Error processing chunk: ${error.message}"
            }
        }

    fun createAnalysisProcessor(analysisType: String): suspend (String, ChunkContext) -> String = { prompt, context ->
        aiAgentService.ask(
            prompt = prompt,
            systemMessage = """
            You are performing $analysisType analysis on a document.
            
            Process each chunk systematically:
            1. Extract relevant information for $analysisType
            2. Consider context from previous chunks
            3. Build comprehensive understanding across chunks
            4. Maintain analytical consistency
            
            Focus on $analysisType while preserving context continuity.
        """.trimIndent(),
            maxTokens = 600,
            temperature = 0.4
        ).getOrElse { error -> "Error processing chunk: ${error.message}" }
    }
}