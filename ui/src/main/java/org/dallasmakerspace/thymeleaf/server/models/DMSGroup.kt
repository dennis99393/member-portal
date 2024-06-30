package org.dallasmakerspace.thymeleaf.server.models

data class DMSGroup(
    val name: String,
    val distinguishedName: String,
    val objectGuid: String?,
    val members: List<DMSMember>?
) {
  companion object {
    fun fromMap(data: Map<String, Any?>): DMSGroup {
      val members =
          (data["members"] as List<*>?)?.filterIsInstance<Map<String, Any?>>()?.map {
            DMSMember.fromMap(it)
          } ?: emptyList()
      return DMSGroup(
          name = data["name"] as String,
          distinguishedName = data["distinguishedName"] as String,
          objectGuid = data["objectGuid"] as String?,
          members = members)
    }
  }
}
