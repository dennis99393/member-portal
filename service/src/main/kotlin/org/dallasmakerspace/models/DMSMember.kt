package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class DMSMember(
    val username: String,
    val avatarUrl: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var groups: List<DMSGroup>? = null
)
