package org.dallasmakerspace.server.models

import java.time.Instant
import kotlin.reflect.full.memberProperties

@Suppress("LongParameterList")
class DMSMember(
    val username: String,
    var firstName: String? = null,
    var lastName: String? = null,
    var displayName: String? = null,
    val avatarUrl: String? = null,
    var personalEmail: String? = null,
    var phoneNumber: String? = null,
    var badgeNumber: String? = null,
    var discourseUsername: String? = null,
    var discourseAvatarUrl: String? = null,
    var discordUserId: String? = null,
    var memberSince: Instant? = null,
    var enabled: Boolean = false,
    var groups: List<DMSGroup> = emptyList()
) {
  override fun toString(): String {
    // Use reflection to generate a string containing all the properties of the class
    return this::class.memberProperties.joinToString(", ") { "${it.name}: ${it.getter.call(this)}" }
  }

  companion object {
    fun fromMap(data: Map<String, Any?>): DMSMember {
      val groups =
          (data["groups"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            if (it["data"] is Map<*, *>) {
              val dataGroup = it["data"] as Map<String, Any?>
              DMSGroup.fromMap(dataGroup)
            } else {
              DMSGroup.fromMap(it as Map<String, Any?>)
            }
          } ?: emptyList()
      return DMSMember(
          username = data["username"] as String,
          firstName = data["firstName"] as String?,
          lastName = data["lastName"] as String?,
          displayName = data["displayName"] as String?,
          avatarUrl = data["avatarUrl"] as String?,
          personalEmail = data["personalEmail"] as String?,
          phoneNumber = data["phoneNumber"] as String?,
          badgeNumber = data["badgeNumber"] as String?,
          enabled = data["enabled"] as Boolean,
          discourseUsername = data["discourseUsername"] as String?,
          discourseAvatarUrl = data["discourseAvatarUrl"] as String?,
          discordUserId = data["discordUserId"] as String?,
          memberSince = Instant.parse(data["memberSince"] as String),
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
