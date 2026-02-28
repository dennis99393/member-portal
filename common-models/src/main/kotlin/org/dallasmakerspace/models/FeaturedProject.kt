package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class FeaturedProject(
    val topicId: Int,
    val postId: Int,
    val title: String,
    val imageUrl: String,
    val memberUsername: String,
    val memberDisplayName: String?,
    val memberAvatarUrl: String?,
    val likeCount: Int,
    val discourseTopicUrl: String
)
