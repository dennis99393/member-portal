package org.dallasmakerspace.discourse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the response when creating a new post/topic in Discourse.
 * Returned from POST /posts.json endpoint.
 */
@Serializable
data class DiscoursePostResponse(
    @SerialName("id") val id: Int,
    @SerialName("topic_id") val topicId: Int,
    @SerialName("topic_slug") val topicSlug: String
)

/**
 * Represents a single topic in search results.
 */
@Serializable
data class DiscourseTopicSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("slug") val slug: String,
    @SerialName("pinned_globally") val pinnedGlobally: Boolean = false,
    @SerialName("pinned") val pinned: Boolean = false,
    @SerialName("closed") val closed: Boolean = false,
    @SerialName("category_id") val categoryId: Int? = null
)

/**
 * Represents the search response from Discourse GET /search.json endpoint.
 */
@Serializable
data class DiscourseSearchResponse(
    @SerialName("topics") val topics: List<DiscourseTopicSearchResult> = emptyList(),
    @SerialName("posts") val posts: List<DiscoursePostSearchResult> = emptyList()
)

/**
 * Represents a single post in search results.
 */
@Serializable
data class DiscoursePostSearchResult(
    @SerialName("id") val id: Int,
    @SerialName("topic_id") val topicId: Int,
    @SerialName("blurb") val blurb: String? = null
)
