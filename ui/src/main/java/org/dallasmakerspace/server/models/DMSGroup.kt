package org.dallasmakerspace.server.models

import io.ktor.http.*
import org.dallasmakerspace.server.models.DMSGroup.Companion.getNameFromSlug
import org.dallasmakerspace.server.models.DMSGroup.Companion.getSlugFromName
import java.time.Instant

data class GroupHistoryEvent(
    val actorUsername: String,
    val memberUsername: String,
    val eventTimestamp: Instant,
) {
  companion object {
    fun fromMap(data: Map<String, Any?>): GroupHistoryEvent {
      val timestampStr = data["eventTimestamp"] as String
      // Handle timestamps that may not have seconds (e.g., "2025-10-08T01:40" ->
      // "2025-10-08T01:40:00")
      val normalizedTimestamp =
          if (timestampStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$"))) {
            "${timestampStr}:00Z"
          } else if (
              !timestampStr.endsWith("Z") &&
                  !timestampStr.contains("+") &&
                  !timestampStr.contains("Z")
          ) {
            "${timestampStr}Z"
          } else {
            timestampStr
          }

      return GroupHistoryEvent(
          actorUsername = data["actorUsername"] as String,
          memberUsername = data["memberUsername"] as String,
          eventTimestamp = Instant.parse(normalizedTimestamp),
      )
    }
  }
}

data class DMSGroup(
    val name: String,
    val description: String?,
    val slug: String, // URL Fragment for the group name
    val distinguishedName: String,
    val objectGuid: String?,
    val membersListIncomplete: Boolean = false,
    val members: List<DMSMember> = emptyList(),
    val administrators: List<String> = emptyList(), // List of DNs (Distinguished Names)
    val history: List<GroupHistoryEvent> = emptyList(),
) {
  companion object {
    fun fromMap(data: Map<String, Any?>): DMSGroup {
      val members =
          (data["members"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            DMSMember.fromMap(it)
          } ?: emptyList()
      val administrators =
          (data["administrators"] as List<*>?)?.filterIsInstance<String>() ?: emptyList()
      val history =
          (data["history"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            GroupHistoryEvent.fromMap(it)
          } ?: emptyList()
      return DMSGroup(
          name = data["name"] as String,
          description = data["description"] as String?,
          distinguishedName = data["distinguishedName"] as String,
          objectGuid = data["objectGuid"] as String?,
          slug = getSlugFromName(data["name"] as String),
          membersListIncomplete = data["membersListIncomplete"] as Boolean,
          members = members,
          administrators = administrators,
          history = history,
      )
    }

    /**
     * Generate a slug (URL Fragment) from a group name. The group name can have spaces that get
     * translated to %20 which results in an ugly URL. So we replace space with "+" to use in the
     * URL. [getNameFromSlug] performs the reverse process.
     */
    fun getSlugFromName(groupName: String) = groupName.replace(" ", "+").encodeURLPath()

    /**
     * Generate a group name from a slug (URL Fragment). The slug can have "+" that get translated
     * to spaces. [getSlugFromName] performs the reverse process.
     */
    fun getNameFromSlug(slug: String) = slug.replace("+", " ").decodeURLPart()

    /**
     * Parse a Distinguished Name (DN) to extract the CN (Common Name). Example: "CN=Domain
     * Admins,OU=Groups,OU=Admin,DC=dms,DC=local" -> "Domain Admins"
     */
    fun parseCNFromDN(dn: String): String {
      val cnPrefix = "CN="
      val startIndex = dn.indexOf(cnPrefix)
      if (startIndex == -1) return dn
      val cnStart = startIndex + cnPrefix.length
      val cnEnd = dn.indexOf(',', cnStart)
      return if (cnEnd != -1) dn.substring(cnStart, cnEnd) else dn.substring(cnStart)
    }

    /**
     * Determine if a DN represents a group (vs. a user). Groups typically have OU=Security or
     * OU=Organizational in their DN. Users typically have OU=Users or OU=People in their DN.
     */
    fun isGroupDN(dn: String): Boolean {
      val lowerDN = dn.lowercase()
      return lowerDN.contains("ou=security") ||
          lowerDN.contains("ou=organizational") ||
          lowerDN.contains("ou=groups")
    }
  }
}
