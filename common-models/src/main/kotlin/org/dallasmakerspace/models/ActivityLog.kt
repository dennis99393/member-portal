package org.dallasmakerspace.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ActivityLog(
    val id: Int? = null,
    val source: ActivityLogSource,
    val actorProfileUsername: String?,
    val subjectProfileUsername: String,
    val event: ActivityLogEvent,
    val attributes: String?,
    val created: LocalDateTime? = null
)

@Suppress("MagicNumber")
/** Event type for an activity log - e.g. login, logout, etc. * */
enum class ActivityLogEvent(val value: Int) {
  UNKNOWN(0),

  /** Profile events * */
  LINK_DISCOURSE(1),
  UNLINK_DISCOURSE(2),
  LINK_DISCORD(3),
  UNLINK_DISCORD(4),
  REMOVE_FROM_DISCOURSE_MEMBERS_GROUP(5),
  ADD_TO_DISCOURSE_MEMBERS_GROUP(6),
  REMOVE_FROM_VOTING_MEMBERS_GROUP(7),
  ADD_TO_VOTING_MEMBERS_GROUP(8),

  /** Label printing events * */
  LABEL_PRINT_CERAMICS(9),
  LABEL_PRINT_CA(10),
  LABEL_PRINT_3DFAB(11),

  /** Badge management events * */
  FIX_BADGE_LENGTH_AD(12),

  /** Group membership events */
  GROUP_MEMBER_ADDED(13),
  GROUP_MEMBER_REMOVED(14),
}

@Suppress("MagicNumber")
/** Source system generating an activity log - e.g. profile, calendar, etc. * */
enum class ActivityLogSource(val value: Int) {
  UNKNOWN(0),
  PROFILE(1),
  CALENDAR(2),
  MEMBER_ACTIVITY_WEBHOOK(3),
}
