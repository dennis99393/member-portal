package org.dallasmakerspace.thymeleaf.server.models

import io.ktor.util.reflect.*
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberServiceException

@Suppress("LongParameterList")
class DMSMember(
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var discordUserId: String? = null,
    var groups: List<DMSGroup> = emptyList(),
) {
  companion object {
    fun fromMap(map: Map<String, Any?>): DMSMember {
      val data =
          map["data"] as Map<*, *>?
              ?: throw MemberServiceException("data attribute missing required")
      val groups =
          (data["groups"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            DMSGroup.fromMap(it)
          } ?: emptyList()
      return DMSMember(
          username = data["username"] as String,
          firstName = data["firstName"] as String?,
          lastName = data["lastName"] as String?,
          displayName = data["displayName"] as String?,
          avatarUrl = data["avatarUrl"] as String?,
          discourseUsername = data["discourseUsername"] as String?,
          discourseAvatarUrl = data["discourseAvatarUrl"] as String?,
          discordUserId = data["discordUserId"] as String?,
          groups = groups)
    }

    fun toMap(member: DMSMember): Map<String, Any?> {
      return mapOf(
          "username" to member.username,
          "avatarUrl" to member.avatarUrl,
          "discourseUsername" to member.discourseUsername,
          "discourseAvatarUrl" to member.discourseAvatarUrl,
          "discordUserId" to member.discordUserId,
      )
    }
  }
}
