package org.dallasmakerspace.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DMSMember(
    val username: String,
    var firstName: String? = null,
    var lastName: String? = null,
    var displayName: String? = null,
    val avatarUrl: String? = null,
    var personalEmail: String? = null,
    var phoneNumber: String? = null,
    var badgeNumber: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var discordUserId: String? = null,
    var memberSince: Instant? = null,
    var groups: List<DMSGroup>? = null
)
