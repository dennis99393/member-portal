package org.dallasmakerspace.routing

import io.ktor.resources.*

@Resource("/members")
class Members(val loggedInDays: Int = 90) {

  @Resource("{username}")
  class DMSMember(
      val parent: Members = Members(),
      val username: String,
      val avatarUrl: String? = null,
      val discourseUsername: String? = null,
      val discourseAvatarUrl: String? = null,
      val discordUserId: String? = null,
  ) {
    @Resource("update") class Update(val parent: DMSMember)

    @Resource("activity")
    class ActivityLog(val parent: DMSMember) {
      @Resource("add") class Add(val parent: ActivityLog)
    }
  }
}
