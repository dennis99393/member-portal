package org.dallasmakerspace.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Represents the type of action performed on a group membership. Maps to Active Directory event
 * IDs: 4728/4732 for member added, 4729/4733 for member removed.
 */
@Serializable
enum class ActionType(val value: Int) {
  ADD_USER(0),
  REMOVE_USER(1);

  companion object {
    fun fromValue(value: Int): ActionType = entries.find { it.value == value } ?: ADD_USER

    /**
     * Maps Active Directory event IDs to ActionType. Event IDs: 4728 = member added to global
     * security group, 4729 = member removed from global security group, 4732 = member added to
     * local security group, 4733 = member removed from local security group
     */
    fun fromEventId(eventId: String): ActionType =
        when (eventId) {
          "4728",
          "4732" -> ADD_USER
          "4729",
          "4733" -> REMOVE_USER
          else -> ADD_USER
        }
  }
}

/**
 * Represents a group membership history record in the system. Tracks changes to group memberships
 * by recording who made the change, which member was affected, and when.
 */
@Serializable
data class GroupHistory(
    val actorUsername: String,
    val memberUsername: String,
    val actionType: ActionType,
    val eventTimestamp: LocalDateTime,
)
