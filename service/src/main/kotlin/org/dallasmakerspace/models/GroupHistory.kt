package org.dallasmakerspace.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents a group membership history record in the system. Tracks changes to group memberships
 * by recording who made the change, which member was affected, and which group was modified.
 */
@Serializable
data class GroupHistory(
    val id: Int,
    val actorUsername: String,
    val memberUsername: String,
    val groupName: String,
    val eventTimestamp: LocalDateTime,
    val created: LocalDateTime
)
