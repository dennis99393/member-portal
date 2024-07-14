package org.dallasmakerspace.members

import javax.inject.Inject
import org.dallasmakerspace.models.ActivityLog

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
}
