package org.dallasmakerspace.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.dallasmakerspace.members.db.ProfileDAO

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
    var memberSince: Instant? = null,
    var enabled: Boolean = false,
    var groups: List<DMSGroup> = emptyList()
)

fun daoToProfileModel(dao: ProfileDAO) =
    DMSMember(
        id = dao.idColumn.value,
        username = dao.id.value,
        enabled = dao.isEnabled,
        avatarUrl = dao.avatarUrl,
        discourseUsername = dao.discourseUsername,
        discourseAvatarUrl = dao.discourseAvatarUrl,
        discordUserId = dao.discordUserId)
