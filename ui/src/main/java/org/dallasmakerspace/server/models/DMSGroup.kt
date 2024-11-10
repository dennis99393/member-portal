package org.dallasmakerspace.server.models

import io.ktor.http.*

data class DMSGroup(
    val name: String,
    val description: String?,
    val slug: String, // URL Fragment for the group name
    val distinguishedName: String,
    val objectGuid: String?,
    val membersListIncomplete: Boolean = false,
    val members: List<DMSMember> = emptyList(),
) {
  companion object {
    fun fromMap(data: Map<String, Any?>): DMSGroup {
      val members =
          (data["members"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            DMSMember.fromMap(it)
          } ?: emptyList()
      return DMSGroup(
          name = data["name"] as String,
          description = data["description"] as String?,
          distinguishedName = data["distinguishedName"] as String,
          objectGuid = data["objectGuid"] as String?,
          slug = getSlugFromName(data["name"] as String),
          membersListIncomplete = data["membersListIncomplete"] as Boolean,
          members = members)
    }

    /**
     * Generate a slug (URL Fragment) from a group name. The group name can have spaces that get
     * translated to %20 which results in an ugly URL. So we replace space with "+" to use in the
     * URL. [getNameFromSlug] performs the reverse process.
     */
    private fun getSlugFromName(groupName: String) = groupName.replace(" ", "+").encodeURLPath()

    /**
     * Generate a group name from a slug (URL Fragment). The slug can have "+" that get translated
     * to spaces. [getSlugFromName] performs the reverse process.
     */
    fun getNameFromSlug(slug: String) = slug.replace("+", " ").decodeURLPart()
  }
}
