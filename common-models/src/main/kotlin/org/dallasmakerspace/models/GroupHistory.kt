package org.dallasmakerspace.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents a group membership history record in the system. Tracks changes to group memberships
 * by recording who made the change, which member was affected, and when.
 */
@Serializable
data class GroupHistory(
    val actorUsername: String,
    val memberUsername: String,
    val eventTimestamp: LocalDateTime,
)
