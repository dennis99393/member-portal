package org.dallasmakerspace.askai

import kotlinx.serialization.json.Json
import org.dallasmakerspace.askai.openrouter.ChatMessage
import org.dallasmakerspace.askai.openrouter.IOpenRouterClient
import org.dallasmakerspace.askai.openrouter.LlmResult
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.models.AskAiCacheEntry
import org.dallasmakerspace.models.ClassificationResult
import org.dallasmakerspace.models.SearchResult

/**
 * Base class for LLM clients. Provides shared prompt-building logic for [classify] and
 * [generateAnswer]; subclasses implement the transport via [chatCompletion].
 */
abstract class AbstractLlmClient(loggerFactory: LoggerFactory) : IOpenRouterClient {
  protected val log = loggerFactory.create(javaClass)

  override fun estimateCost(inputTokens: Int, outputTokens: Int): Double = 0.0

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  override suspend fun classify(
      question: String,
      cachedQuestions: List<AskAiCacheEntry>,
  ): LlmResult<ClassificationResult> {
    val cachedQuestionsText =
        if (cachedQuestions.isEmpty()) {
          "No previously answered questions available."
        } else {
          cachedQuestions.joinToString("\n") { "- ID: ${it.id}, Question: \"${it.questionText}\"" }
        }

    val systemPrompt =
        """You are a question classifier for Dallas Makerspace. Given a user question and a list of previously answered questions, either:
1. Return the ID of a matching cached question if the user's question is semantically equivalent (asking about the same thing)
2. Return 1-3 search queries to find relevant information if no match is found

Previously answered questions:
$cachedQuestionsText

Respond ONLY with valid JSON in this exact format:
{
  "matchedCacheId": <number or null>,
  "searchQueries": ["query1", "query2"]
}

If you find a matching cached question, set matchedCacheId and leave searchQueries empty.
If no match, set matchedCacheId to null and provide 1-3 search queries."""

    val messages =
        listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = "User question: \"$question\""),
        )

    val llmResult = chatCompletion(messages, jsonMode = true)

    val classificationResult =
        try {
          json.decodeFromString<ClassificationResult>(llmResult.result)
        } catch (e: Exception) {
          log.warn("Failed to parse classification response: ${llmResult.result}", e)
          ClassificationResult(matchedCacheId = null, searchQueries = listOf(question))
        }

    return LlmResult(
        result = classificationResult,
        usage = llmResult.usage,
        modelName = llmResult.modelName,
    )
  }

  override suspend fun generateAnswer(
      question: String,
      searchResults: List<SearchResult>,
  ): LlmResult<String> {
    val searchResultsText =
        if (searchResults.isEmpty()) {
          "No search results found."
        } else {
          searchResults
              .mapIndexed { index, result ->
                """
            |[${index + 1}] Source: ${result.source} - ${result.title}
            |URL: ${result.url}
            |Content: ${result.snippet}
            |---"""
                    .trimMargin()
              }
              .joinToString("\n")
        }

    val systemPrompt =
        """You are a helpful assistant for Dallas Makerspace members. Based on the search results below, provide a clear, concise answer to the user's question.

Guidelines:
- Be concise and direct
- If the search results don't contain enough information to answer the question, say so clearly
- Reference specific sources using «1», «2», etc. (e.g., "According to «1»...")
- Format your response in markdown for readability
- Use bullet points or numbered lists when appropriate
- Do not make up information not present in the search results

Search Results:
$searchResultsText"""

    val messages =
        listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = "Question: \"$question\""),
        )

    return chatCompletion(messages, jsonMode = false)
  }
}
