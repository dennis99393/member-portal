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
      val discordUsername: String? = null,
      val discordAvatarUrl: String? = null,
      val discordWebhookId: String? = null,
      val linkedinUsername: String? = null,
  ) {
    @Resource("update") class Update(val parent: DMSMember)

    @Resource("activity")
    class ActivityLog(val parent: DMSMember) {
      @Resource("add") class Add(val parent: ActivityLog)
    }

    @Resource("events") class Events(val parent: DMSMember, val limit: Int = 5)

    @Resource("badge-mm") class BadgeMM(val parent: DMSMember)

    @Resource("badge-ad") class BadgeAD(val parent: DMSMember)

    @Resource("fix-badge") class FixBadge(val parent: DMSMember)
  }
}
