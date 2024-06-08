package org.dallasmakerspace.activedirectory

import dagger.Reusable
import javax.inject.Inject

private const val i = 3

@Reusable
class ActiveDirectoryService
@Inject
constructor(private val activeDirectoryClient: ActiveDirectoryClient) {
  fun getMember(username: String): ADUser {
    val memberMap: Map<String, Any?> = activeDirectoryClient.getUser(username)
    val memberOf = memberMap["memberOf"]
    val groups =
        if (memberOf is Array<*>) {
          val list = memberOf.toList()
          parseGroups(list.filterIsInstance<String>())
        } else {
          emptyList()
        }
    return ADUser(
        cn = memberMap["cn"].toString(),
        mail = memberMap["mail"].toString(),
        objectGuid = memberMap["objectGuid"].toString(),
        groups = groups)
  }

  /**
   * Parses the list of group names into a list of ADGroup objects.
   *
   * @param list The list of group names in distinguishedName format
   *   "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local".
   * @return The list of ADGroup objects. Empty list if the input list is null.
   */
  private fun parseGroups(list: List<String>?): List<ADGroup> {
    return list?.map {
      val parts = it.split(",")
      val startIndex = 3 // to remove "cn=" from the start of the string.
      val cn = parts.first().substring(startIndex)
      val dn = it
      ADGroup(cn, distinguishedName = dn, objectGuid = null)
    } ?: emptyList()
  }
}
