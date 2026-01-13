package org.dallasmakerspace.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Request model for asking a question to the AI assistant. */
@Serializable
data class AskAiRequest(
    val question: String,
)

/** Request model for submitting feedback on an AI answer. */
@Serializable
data class AskAiFeedbackRequest(
    val cacheId: Int,
    val memberId: Int? = null,
    val isHelpful: Boolean,
)

/** Response model from the AI assistant. */
@Serializable
data class AskAiResponse(
    val answer: String,
    val sources: List<SourceLink>,
    val fromCache: Boolean,
    val slug: String? = null,
    val cacheId: Int? = null,
    val askedByUsername: String? = null,
    val metadata: AskAiMetadata? = null,
    /** Additional resources (PDFs, attachments) that weren't used in answer generation. */
    val additionalResources: List<SourceLink> = emptyList(),
)

/** Metadata about the AI response generation process. */
@Serializable
data class AskAiMetadata(
    val searchQueries: List<String> = emptyList(),
    val sourceBreakdown: Map<String, Int> = emptyMap(),
    val classificationTokens: TokenUsage? = null,
    val answerTokens: TokenUsage? = null,
    val estimatedCostUsd: Double? = null,
    val modelName: String? = null,
)

/** Token usage for an LLM call. */
@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

/** A link to a source document that was used to generate the answer. */
@Serializable
data class SourceLink(
    val title: String,
    val url: String,
    val source: String,
)

/** Cached question-answer pair stored in the database. */
@Serializable
data class AskAiCacheEntry(
    val id: Int,
    val slug: String,
    val questionHash: String,
    val questionText: String,
    val answerText: String,
    val sources: List<SourceLink>,
    val askedByUsername: String?,
    val createdAt: Instant,
    val hitCount: Int,
    val lastHitAt: Instant?,
    val metadata: AskAiMetadata? = null,
)

/** Result from the classification LLM call. */
@Serializable
data class ClassificationResult(
    val matchedCacheId: Int? = null,
    val searchQueries: List<String> = emptyList(),
)

/** Search result from a search source (Discourse, Confluence, etc.). */
@Serializable
data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String,
    val relevanceScore: Double? = null,
    /** Whether this result has extractable text content for LLM processing. */
    val hasExtractableContent: Boolean = true,
)
