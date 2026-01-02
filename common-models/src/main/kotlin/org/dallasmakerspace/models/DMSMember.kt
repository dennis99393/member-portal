package org.dallasmakerspace.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DMSMember(
    val id: Int,
    val username: String,
    var firstName: String? = null,
    var lastName: String? = null,
    var displayName: String? = null,
    var avatarUrl: String? = null,
    var personalEmail: String? = null,
    var phoneNumber: String? = null,
    var badgeNumber: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var discordUserId: String? = null,
    var discordUsername: String? = null,
    var discordAvatarUrl: String? = null,
    var discordWebhookId: String? = null,
    var memberSince: Instant? = null,
    var enabled: Boolean = false,
    var groups: List<DMSGroup> = emptyList(),
    var accountInfo: AccountInfo? = null,
)
