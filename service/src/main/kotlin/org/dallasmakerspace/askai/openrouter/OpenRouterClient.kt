package org.dallasmakerspace.askai.openrouter

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okio.IOException
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.models.AskAiCacheEntry
import org.dallasmakerspace.models.ClassificationResult
import org.dallasmakerspace.models.SearchResult
import org.dallasmakerspace.models.TokenUsage

private const val REQUEST_TIMEOUT_MS = 60000L

@Singleton
class OpenRouterClient
@Inject
constructor(private val appConfig: AppConfig, loggerFactory: LoggerFactory) : IOpenRouterClient {
  private val log = loggerFactory.create(javaClass)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val baseUrl: String by lazy { appConfig.requireStringProperty("app.openrouter.baseUrl") }

  private val apiKey: String by lazy { appConfig.requireStringProperty("app.openrouter.apiKey") }

  private val model: String by lazy { appConfig.requireStringProperty("app.openrouter.model") }

  private fun getClient() =
      HttpClient(CIO) {
        install(HttpTimeout) {
          requestTimeoutMillis = REQUEST_TIMEOUT_MS
          connectTimeoutMillis = REQUEST_TIMEOUT_MS
          socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(Logging) {
          logger = Logger.DEFAULT
          level = LogLevel.INFO
        }
      }

  override suspend fun classify(
      question: String,
      cachedQuestions: List<AskAiCacheEntry>
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

    val userPrompt = "User question: \"$question\""

    val messages =
        listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt))

    val llmResult = chatCompletion(messages, jsonMode = true)

    val classificationResult =
        try {
          json.decodeFromString<ClassificationResult>(llmResult.result)
        } catch (e: Exception) {
          log.warn("Failed to parse classification response: ${llmResult.result}", e)
          // Fallback: generate basic search queries from the question
          ClassificationResult(matchedCacheId = null, searchQueries = listOf(question))
        }

    return LlmResult(result = classificationResult, usage = llmResult.usage)
  }

  override suspend fun generateAnswer(
      question: String,
      searchResults: List<SearchResult>
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

    val userPrompt = "Question: \"$question\""

    val messages =
        listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = userPrompt))

    return chatCompletion(messages, jsonMode = false)
  }

  override suspend fun chatCompletion(
      messages: List<ChatMessage>,
      jsonMode: Boolean
  ): LlmResult<String> {
    val request =
        OpenRouterChatRequest(
            model = model,
            messages = messages,
            responseFormat = if (jsonMode) ResponseFormat(type = "json_object") else null)

    val requestBody = json.encodeToString(OpenRouterChatRequest.serializer(), request)

    try {
      val response =
          getClient().use {
            it.request("$baseUrl/chat/completions") {
              method = HttpMethod.Post
              header("Authorization", "Bearer $apiKey")
              header("HTTP-Referer", "https://portal.dallasmakerspace.org")
              header("X-Title", "Dallas Makerspace Member Portal")
              contentType(ContentType.Application.Json)
              setBody(requestBody)
            }
          }

      when (response.status) {
        HttpStatusCode.OK -> {
          val responseBody = response.bodyAsText()
          val chatResponse = json.decodeFromString<OpenRouterChatResponse>(responseBody)
          val content =
              chatResponse.choices.firstOrNull()?.message?.content
                  ?: throw OpenRouterApiException("No response content from OpenRouter")
          val usage =
              chatResponse.usage?.let {
                TokenUsage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens)
              }
          return LlmResult(result = content, usage = usage)
        }
        HttpStatusCode.TooManyRequests -> {
          throw OpenRouterApiException("Rate limited by OpenRouter API")
        }
        HttpStatusCode.Unauthorized -> {
          throw OpenRouterApiException("Invalid OpenRouter API key")
        }
        else -> {
          throw OpenRouterApiException(
              "OpenRouter API error: ${response.status} - ${response.bodyAsText()}")
        }
      }
    } catch (e: IOException) {
      throw OpenRouterApiException("Failed to connect to OpenRouter API", e)
    } catch (e: kotlinx.serialization.SerializationException) {
      throw OpenRouterApiException("Failed to parse OpenRouter response", e)
    }
  }
}

/** Exception thrown when OpenRouter API calls fail. */
class OpenRouterApiException : IOException {
  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)
}
