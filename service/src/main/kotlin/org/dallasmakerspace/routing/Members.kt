package org.dallasmakerspace.routing

import io.ktor.resources.*

@Resource("/members")
class Members(val sort: String? = "new") {

  @Resource("{username}")
  class DMSMember(
      val parent: Members = Members(),
      val username: String,
      val discourseUsername: String? = null,
      val discourseAvatarUrl: String? = null
  ) {
    override fun toString(): String {
      return "DMSMember(username='$username', discourseUsername=$discourseUsername)"
    }

    @Resource("update") class Update(val parent: DMSMember)
  }
}
