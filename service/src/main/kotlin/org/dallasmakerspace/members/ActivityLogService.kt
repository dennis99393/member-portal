package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.models.ActivityLog
import org.dallasmakerspace.models.ActivityLogEvent
import org.dallasmakerspace.models.ActivityLogSource

class ActivityLogService
@Inject
constructor(
    private val memberRepository: MemberRepository,
    private val activityLogRepository: ActivityLogRepository
) {
  /**
   * Fetches the activity log for a member.
   *
   * @param username The username of the member.
   * @return The activity log for the member.
   */
  suspend fun getMemberActivityLog(username: String): List<ActivityLog> {
    val member = memberRepository.getMemberOrInsert(username)
    return activityLogRepository.getMemberActivityLog(member.id)
  }

  /** Inserts a new activity log entry. */
  suspend fun insertActivityLogEntry(subjectUsername: String, event: ActivityLogEvent) {
    // TODO(mandarl): add actor user context here
    activityLogRepository.insertActivityLogEntry(
        ActivityLog(
            source = ActivityLogSource.PROFILE,
            actorProfileUsername = subjectUsername,
            subjectProfileUsername = subjectUsername,
            event = event,
            attributes = null))
  }
}
