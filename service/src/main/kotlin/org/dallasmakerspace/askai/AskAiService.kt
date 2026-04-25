package org.dallasmakerspace.askai

import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.dallasmakerspace.askai.db.AskAiCacheRepository
import org.dallasmakerspace.askai.openrouter.IOpenRouterClient
import org.dallasmakerspace.askai.search.SearchSourceRouter
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.members.MemberRepository
import org.dallasmakerspace.models.AskAiMetadata
import org.dallasmakerspace.models.AskAiResponse
import org.dallasmakerspace.models.SourceLink
import org.dallasmakerspace.models.TokenUsage

/**
 * Configuration for AskAI service parameters.
 *
 * @param topQuestionsLimit Number of recent questions to use for semantic matching
 * @param resultsPerSource Maximum search results to fetch per source
 * @param maxTotalResults Maximum total search results across all sources
 * @param searchTimeoutSeconds Timeout for search operations
 * @param llmRetryAttempts Number of retry attempts for LLM calls
 * @param earlyStopThreshold Stop searching if we have this many quality results
 */
data class AskAiConfig(
    val topQuestionsLimit: Int = 20,
    val resultsPerSource: Int = 5,
    val maxTotalResults: Int = 15,
    val searchTimeoutSeconds: Long = 5,
    val llmRetryAttempts: Int = 3,
    val earlyStopThreshold: Int = 10,
)

/**
 * Metrics tracked for each AskAI request.
 *
 * @param requestId Unique identifier for this request
 * @param startTime When the request started
 * @param cacheHit Whether the request was served from cache
 * @param cacheType Type of cache hit ("exact" or "semantic")
 * @param searchCount Number of search queries executed
 * @param totalCost Estimated cost in USD
 * @param responseTimeMs Total response time in milliseconds
 */
data class RequestMetrics(
    val requestId: String = UUID.randomUUID().toString(),
    val startTime: Instant = Instant.now(),
    var cacheHit: Boolean = false,
    var cacheType: String? = null,
    var searchCount: Int = 0,
    var totalCost: Double = 0.0,
    var responseTimeMs: Long = 0,
)

/**
 * Main service for the Ask DMS AI feature. Orchestrates the two-LLM-call flow:
 * 1. Check cache for exact match
 * 2. LLM #1: Classify question and generate search queries (or find semantic match)
 * 3. Execute search queries across all sources
 * 4. LLM #2: Generate answer from search results
 * 5. Cache the result for future use
 */
@Singleton
class AskAiService
@Inject
constructor(
    private val openRouterClient: IOpenRouterClient,
    private val searchSourceRouter: SearchSourceRouter,
    private val cacheRepository: AskAiCacheRepository,
    private val memberRepository: MemberRepository,
    private val piiMasker: PiiMasker,
    private val config: AskAiConfig,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  /**
   * Process a user question and return an AI-generated answer.
   *
   * @param question The user's question
   * @param memberId Optional member ID for tracking (currently unused)
   * @param username Username of the member asking the question (required)
   * @param forceRefresh If true, bypass cache and generate a fresh answer (dev mode only)
   * @return AskAiResponse containing the answer, sources, and cache status
   * @throws IllegalArgumentException if username is not provided
   */
  suspend fun ask(
      question: String,
      memberId: Int? = null,
      username: String? = null,
      forceRefresh: Boolean = false,
  ): AskAiResponse {
    val metrics = RequestMetrics()
    log.info("Processing question (requestId=${metrics.requestId}): $question")

    requireNotNull(username) { "Username is required to ask a question" }

    // Look up profile_id from username upfront
    val profileId =
        try {
          memberRepository.getMemberOrInsert(username, enabled = null).id
        } catch (e: Exception) {
          log.error("Failed to look up profile for username: $username", e)
          throw IllegalArgumentException("Could not find profile for username: $username", e)
        }

    // Step 1: Check cache for exact match (fast path) - skip if forceRefresh is true
    if (!forceRefresh) {
      val exactMatch = cacheRepository.findByExactQuestion(question)
      if (exactMatch != null) {
        metrics.cacheHit = true
        metrics.cacheType = "exact"
        metrics.responseTimeMs = Duration.between(metrics.startTime, Instant.now()).toMillis()
        log.info(
            "Found exact cache match for question (id=${exactMatch.id}, requestId=${metrics.requestId}, responseTime=${metrics.responseTimeMs}ms)")
        cacheRepository.incrementHitCount(exactMatch.id)
        // Reorder sources so cited ones appear first
        val orderedSources = reorderSourcesByCitation(exactMatch.answerText, exactMatch.sources)
        return AskAiResponse(
            answer = exactMatch.answerText,
            sources = orderedSources,
            fromCache = true,
            slug = exactMatch.slug,
            cacheId = exactMatch.id,
            askedByUsername = exactMatch.askedByUsername,
            metadata = exactMatch.metadata,
        )
      }
    } else {
      log.info("Force refresh requested - bypassing cache lookup")
    }

    // Step 2: LLM #1 - Classify question and generate search queries
    // Mask PII in the question before sending to LLM
    val maskedQuestion = piiMasker.mask(question)

    // Skip cached questions when force-refreshing — passing them causes the LLM to return a
    // semantic match with empty searchQueries, leaving us with no search phrases to use.
    val maskedCachedQuestions =
        if (forceRefresh) {
          emptyList()
        } else {
          cacheRepository.getTopQuestions(limit = config.topQuestionsLimit).map { entry ->
            entry.copy(questionText = piiMasker.mask(entry.questionText))
          }
        }

    val classificationLlmResult =
        retryWithBackoff(config.llmRetryAttempts) {
          openRouterClient.classify(maskedQuestion, maskedCachedQuestions)
        }
    val classificationResult = classificationLlmResult.result

    // Check if LLM found a semantic match in cached questions - skip if forceRefresh is true
    if (!forceRefresh) {
      val matchedCacheId = classificationResult.matchedCacheId
      if (matchedCacheId != null) {
        val cachedEntry = cacheRepository.getById(matchedCacheId)
        if (cachedEntry != null) {
          metrics.cacheHit = true
          metrics.cacheType = "semantic"
          metrics.totalCost = calculateEstimatedCost(classificationLlmResult.usage, null)
          metrics.responseTimeMs = Duration.between(metrics.startTime, Instant.now()).toMillis()
          log.info(
              "LLM found semantic cache match (id=${cachedEntry.id}, requestId=${metrics.requestId}, cost=\$${String.format("%.4f", metrics.totalCost)}, responseTime=${metrics.responseTimeMs}ms)")
          cacheRepository.incrementHitCount(cachedEntry.id)
          // Reorder sources so cited ones appear first
          val orderedSources = reorderSourcesByCitation(cachedEntry.answerText, cachedEntry.sources)
          return AskAiResponse(
              answer = cachedEntry.answerText,
              sources = orderedSources,
              fromCache = true,
              slug = cachedEntry.slug,
              cacheId = cachedEntry.id,
              askedByUsername = cachedEntry.askedByUsername,
              metadata = cachedEntry.metadata,
          )
        }
      }
    }

    // Step 3: Execute search queries across all sources
    val searchQueries = classificationResult.searchQueries.ifEmpty { listOf(question) }
    metrics.searchCount = searchQueries.size
    log.info("Executing ${searchQueries.size} search queries: $searchQueries")

    // Circuit breaker: execute searches with timeout and graceful degradation
    val allSearchResults =
        searchQueries
            .flatMap { query ->
              try {
                withTimeout(config.searchTimeoutSeconds * 1000L) {
                  searchSourceRouter.searchAll(query, limitPerSource = config.resultsPerSource)
                }
              } catch (e: Exception) {
                if (e is CancellationException) throw e
                log.warn("Search failed for query: $query", e)
                // Continue with remaining queries - graceful degradation
                emptyList()
              }
            }
            .distinctBy { it.url } // Deduplicate by URL

    // Early stopping: limit to configured threshold
    val deduplicatedResults = allSearchResults.take(config.maxTotalResults)

    log.debug("Found ${deduplicatedResults.size} unique search results")

    // Separate extractable content (for LLM) from non-extractable (PDFs, attachments)
    val (extractableResults, additionalResources) =
        deduplicatedResults.partition { it.hasExtractableContent }
    log.debug(
        "Extractable: ${extractableResults.size}, Additional resources: ${additionalResources.size}")

    // If no search results found, return canned response directing user to Talk forum
    if (extractableResults.isEmpty() && additionalResources.isEmpty()) {
      metrics.totalCost = calculateEstimatedCost(classificationLlmResult.usage, null)
      metrics.responseTimeMs = Duration.between(metrics.startTime, Instant.now()).toMillis()
      log.info(
          "No search results found, returning canned response (requestId=${metrics.requestId}, searchQueries=$searchQueries, cost=\$${String.format("%.4f", metrics.totalCost)}, responseTime=${metrics.responseTimeMs}ms)")

      val searchedSourcesList =
          deduplicatedResults.map { it.source }.distinct().sorted().joinToString(", ")
      val searchedSources =
          if (searchedSourcesList.isNotEmpty()) "I searched: $searchedSourcesList"
          else "No search sources were available"

      val cannedAnswer =
          """
          I couldn't find any relevant information to answer your question.

          **What I searched for:**
          ${searchQueries.joinToString("\n") { "* \"$it\"" }}

          **Where I looked:**
          $searchedSources

          I recommend posting your question in the [Ask Dallas Makerspace](https://talk.dallasmakerspace.org/c/ask-dallas-makerspace/82) category on our Talk forum, where community members and staff can help you directly.

          When posting, please include:
          * Any specific details about your question
          * What you've already tried (if applicable)
          * Any relevant photos or links

          Our community is very helpful and you'll usually get a response within a few hours!
          """
              .trimIndent()

      return AskAiResponse(
          answer = cannedAnswer,
          sources = emptyList(),
          fromCache = false,
          slug = null,
          cacheId = null,
          askedByUsername = username,
          metadata =
              AskAiMetadata(
                  searchQueries = searchQueries,
                  sourceBreakdown = emptyMap(),
                  classificationTokens = classificationLlmResult.usage,
                  answerTokens = null,
                  estimatedCostUsd = metrics.totalCost,
                  modelName = classificationLlmResult.modelName,
              ),
          additionalResources = emptyList(),
      )
    }

    // Step 4: LLM #2 - Generate answer from search results
    // Only send extractable content to the LLM, with PII masked
    val maskedSearchResults =
        extractableResults.map { result ->
          result.copy(
              title = piiMasker.mask(result.title),
              snippet = piiMasker.mask(result.snippet),
          )
        }

    val answerLlmResult =
        retryWithBackoff(config.llmRetryAttempts) {
          openRouterClient.generateAnswer(maskedQuestion, maskedSearchResults)
        }
    val answer = answerLlmResult.result

    // Step 5: Cache the result
    // Sources used in answer generation (extractable content only)
    val sources = extractableResults.map { SourceLink(it.title, it.url, it.source) }
    // Reorder sources so cited ones appear first
    val orderedSources = reorderSourcesByCitation(answer, sources)
    // Additional resources not used in generation (PDFs, attachments)
    val additionalResourceLinks =
        additionalResources.map { SourceLink(it.title, it.url, it.source) }

    // Build metadata with search queries, token usage, and cost estimate
    val estimatedCost = calculateEstimatedCost(classificationLlmResult.usage, answerLlmResult.usage)
    val sourceBreakdown = deduplicatedResults.groupingBy { it.source }.eachCount()
    log.debug("Source breakdown: $sourceBreakdown")

    // Update metrics
    metrics.totalCost = estimatedCost
    metrics.responseTimeMs = Duration.between(metrics.startTime, Instant.now()).toMillis()

    val metadata =
        AskAiMetadata(
            searchQueries = searchQueries,
            sourceBreakdown = sourceBreakdown,
            classificationTokens = classificationLlmResult.usage,
            answerTokens = answerLlmResult.usage,
            estimatedCostUsd = estimatedCost,
            modelName = answerLlmResult.modelName,
        )

    var slug: String? = null
    var cacheId: Int? = null

    try {
      // Cache all sources together for future reference
      val allSources = orderedSources + additionalResourceLinks
      val cached = cacheRepository.save(question, answer, allSources, profileId, metadata)
      slug = cached.slug
      cacheId = cached.id
      log.info(
          "Cached new answer for question (slug=$slug, cacheId=$cacheId, profileId=$profileId)")
    } catch (e: Exception) {
      log.warn("Failed to cache answer", e)
    }

    log.info(
        "Generated fresh answer (requestId=${metrics.requestId}, searchQueries=${metrics.searchCount}, sources=${sourceBreakdown.size}, cost=\$${String.format("%.4f", metrics.totalCost)}, responseTime=${metrics.responseTimeMs}ms)")

    return AskAiResponse(
        answer = answer,
        sources = orderedSources,
        fromCache = false,
        slug = slug,
        cacheId = cacheId,
        askedByUsername = username,
        metadata = metadata,
        additionalResources = additionalResourceLinks,
    )
  }

  /**
   * Record user feedback for a cached answer.
   *
   * @param cacheId The cache entry ID
   * @param memberId The member providing feedback
   * @param isHelpful Whether the answer was helpful
   */
  suspend fun recordFeedback(cacheId: Int, memberId: Int, isHelpful: Boolean) {
    cacheRepository.recordFeedback(cacheId, memberId, isHelpful)
    log.info("Recorded feedback for cache entry $cacheId: helpful=$isHelpful")
  }

  /** Get the list of registered search sources. */
  fun getRegisteredSources(): List<String> = searchSourceRouter.getRegisteredSources()

  /**
   * Get recent cached questions without negative feedback for display on the initial page.
   *
   * @param limit Maximum number of questions to return
   * @return List of recent questions without negative feedback
   */
  suspend fun getTopQuestions(limit: Int = 10): List<TopQuestion> {
    return cacheRepository.getRecentQuestionsWithoutNegativeFeedback(limit).map { entry ->
      TopQuestion(
          id = entry.id,
          slug = entry.slug,
          question = entry.questionText,
          hitCount = entry.hitCount,
      )
    }
  }

  /**
   * Get a cached answer by its slug.
   *
   * @param slug The URL-friendly slug
   * @return AskAiResponse if found, null otherwise
   */
  suspend fun getBySlug(slug: String): AskAiResponse? {
    val entry = cacheRepository.findBySlug(slug) ?: return null
    cacheRepository.incrementHitCount(entry.id)
    // Reorder sources so cited ones appear first
    val orderedSources = reorderSourcesByCitation(entry.answerText, entry.sources)
    return AskAiResponse(
        answer = entry.answerText,
        sources = orderedSources,
        fromCache = true,
        slug = entry.slug,
        cacheId = entry.id,
        askedByUsername = entry.askedByUsername,
        metadata = entry.metadata,
    )
  }

  private fun calculateEstimatedCost(
      classificationUsage: TokenUsage?,
      answerUsage: TokenUsage?,
  ): Double {
    val inputTokens = (classificationUsage?.promptTokens ?: 0) + (answerUsage?.promptTokens ?: 0)
    val outputTokens =
        (classificationUsage?.completionTokens ?: 0) + (answerUsage?.completionTokens ?: 0)
    return openRouterClient.estimateCost(inputTokens, outputTokens)
  }

  /**
   * Reorder sources so that cited sources appear first (in order of first citation in answer),
   * followed by uncited sources.
   *
   * Citations in the answer can be in multiple formats due to LLM variance:
   * - Square brackets: [1], [2], [3]
   * - Guillemets: «1», «2», «3» Citations are 1-indexed, while the sources list is 0-indexed.
   */
  private fun reorderSourcesByCitation(
      answer: String,
      sources: List<SourceLink>,
  ): List<SourceLink> {
    // Support multiple citation formats due to LLM variance
    val citationPatterns =
        listOf(
            Regex("""\[(\d+)\]"""), // Square brackets: [1]
            Regex("""«(\d+)»"""), // Guillemets: «1»
        )

    // Find all citations across all patterns
    val allMatches =
        citationPatterns
            .flatMap { pattern -> pattern.findAll(answer).toList() }
            .sortedBy { it.range.first }

    val citedIndices =
        allMatches
            .map { it.groupValues[1].toInt() - 1 } // Convert 1-indexed citations to 0-indexed
            .filter { it in sources.indices } // Only valid indices
            .distinct() // Remove duplicates, keeping first occurrence order
            .toList()

    if (citedIndices.isEmpty()) {
      // No citations found, return sources as-is
      return sources
    }

    // Split into cited and uncited sources
    val citedSources = citedIndices.map { sources[it] }
    val uncitedSources = sources.filterIndexed { index, _ -> index !in citedIndices }

    return citedSources + uncitedSources
  }

  /**
   * Retry a suspending block with exponential backoff.
   *
   * @param maxAttempts Maximum number of retry attempts
   * @param block The suspending function to retry
   * @return Result of the block
   * @throws Exception if all attempts fail
   */
  private suspend fun <T> retryWithBackoff(maxAttempts: Int = 3, block: suspend () -> T): T {
    repeat(maxAttempts - 1) { attempt ->
      try {
        return block()
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        val delayMs = 100L * (1 shl attempt) // Exponential backoff: 100ms, 200ms, 400ms...
        log.warn("Attempt ${attempt + 1} failed, retrying in ${delayMs}ms", e)
        delay(delayMs)
      }
    }
    return block() // Last attempt without catch
  }

}

/** Summary of a cached question for display in the UI. */
@kotlinx.serialization.Serializable
data class TopQuestion(
    val id: Int,
    val slug: String,
    val question: String,
    val hitCount: Int,
)
