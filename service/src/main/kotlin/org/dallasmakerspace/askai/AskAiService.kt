package org.dallasmakerspace.askai

import javax.inject.Inject
import javax.inject.Singleton
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
    loggerFactory: LoggerFactory
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
      forceRefresh: Boolean = false
  ): AskAiResponse {
    log.info("Processing question: $question")

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
        log.info("Found exact cache match for question (id=${exactMatch.id})")
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
            metadata = exactMatch.metadata)
      }
    } else {
      log.info("Force refresh requested - bypassing cache lookup")
    }

    // Step 2: LLM #1 - Classify question and generate search queries
    // Mask PII in the question before sending to LLM
    val maskedQuestion = piiMasker.mask(question)

    val topCachedQuestions = cacheRepository.getTopQuestions(limit = 20)
    // Mask PII in cached questions before sending to LLM
    val maskedCachedQuestions =
        topCachedQuestions.map { entry ->
          entry.copy(questionText = piiMasker.mask(entry.questionText))
        }

    val classificationLlmResult = openRouterClient.classify(maskedQuestion, maskedCachedQuestions)
    val classificationResult = classificationLlmResult.result

    // Check if LLM found a semantic match in cached questions - skip if forceRefresh is true
    if (!forceRefresh) {
      val matchedCacheId = classificationResult.matchedCacheId
      if (matchedCacheId != null) {
        val cachedEntry = cacheRepository.getById(matchedCacheId)
        if (cachedEntry != null) {
          log.info("LLM found semantic cache match (id=${cachedEntry.id})")
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
              metadata = cachedEntry.metadata)
        }
      }
    }

    // Step 3: Execute search queries across all sources
    val searchQueries = classificationResult.searchQueries.ifEmpty { listOf(question) }
    log.info("Executing ${searchQueries.size} search queries: $searchQueries")

    val allSearchResults =
        searchQueries
            .flatMap { query -> searchSourceRouter.searchAll(query, limitPerSource = 5) }
            .distinctBy { it.url } // Deduplicate by URL
            .take(15) // Limit total results

    log.debug("Found ${allSearchResults.size} unique search results")

    // Separate extractable content (for LLM) from non-extractable (PDFs, attachments)
    val (extractableResults, additionalResources) =
        allSearchResults.partition { it.hasExtractableContent }
    log.debug(
        "Extractable: ${extractableResults.size}, Additional resources: ${additionalResources.size}")

    // If no search results found, return canned response directing user to Talk forum
    if (extractableResults.isEmpty() && additionalResources.isEmpty()) {
      log.info("No search results found, returning canned response")
      val cannedAnswer =
          """
          I couldn't find any relevant information in our documentation or forum discussions to answer your question.

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
                  estimatedCostUsd = calculateEstimatedCost(classificationLlmResult.usage, null),
                  modelName = classificationLlmResult.modelName),
          additionalResources = emptyList())
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

    val answerLlmResult = openRouterClient.generateAnswer(maskedQuestion, maskedSearchResults)
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
    val sourceBreakdown = allSearchResults.groupingBy { it.source }.eachCount()
    log.debug("Source breakdown: $sourceBreakdown")

    val metadata =
        AskAiMetadata(
            searchQueries = searchQueries,
            sourceBreakdown = sourceBreakdown,
            classificationTokens = classificationLlmResult.usage,
            answerTokens = answerLlmResult.usage,
            estimatedCostUsd = estimatedCost,
            modelName = answerLlmResult.modelName)

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

    return AskAiResponse(
        answer = answer,
        sources = orderedSources,
        fromCache = false,
        slug = slug,
        cacheId = cacheId,
        askedByUsername = username,
        metadata = metadata,
        additionalResources = additionalResourceLinks)
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
        metadata = entry.metadata)
  }

  /**
   * Calculate estimated cost based on token usage. Pricing: $0.1 per million input tokens, $0.3 per
   * million output tokens.
   */
  private fun calculateEstimatedCost(
      classificationUsage: TokenUsage?,
      answerUsage: TokenUsage?
  ): Double {
    val inputTokens = (classificationUsage?.promptTokens ?: 0) + (answerUsage?.promptTokens ?: 0)
    val outputTokens =
        (classificationUsage?.completionTokens ?: 0) + (answerUsage?.completionTokens ?: 0)

    val inputCost = inputTokens * COST_PER_INPUT_TOKEN
    val outputCost = outputTokens * COST_PER_OUTPUT_TOKEN

    return inputCost + outputCost
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
      sources: List<SourceLink>
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

  companion object {
    // Pricing: $0.1 per million input tokens = $0.0000001 per token
    private const val COST_PER_INPUT_TOKEN = 0.0000001
    // Pricing: $0.3 per million output tokens = $0.0000003 per token
    private const val COST_PER_OUTPUT_TOKEN = 0.0000003
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
