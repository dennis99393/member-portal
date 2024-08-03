package org.dallasmakerspace.activedirectory

import dagger.Reusable
import javax.inject.Inject

private const val GROUP_NAME_PREFIX_LENGTH = 3

@Reusable
class ActiveDirectoryService
@Inject
constructor(private val activeDirectoryClient: IActiveDirectoryClient) : IActiveDirectoryService {

  /** {@inheritDoc} */
  override fun getMember(username: String): ADUser {
    return getMembers(listOf(username))[username]
        ?: throw ADException("User $username not found in AD")
  }

  /** {@inheritDoc} */
  override fun getMembers(usernameList: List<String>): Map<String, ADUser?> {
    val adSearchResult: Map<String, Map<String, Any?>> =
        activeDirectoryClient.getUsers(usernameList)

    // For each username in the list, get the ADUser object.
    return usernameList.associateWith { username ->
      val memberMap = adSearchResult[username] ?: return@associateWith null
      val memberOf = memberMap["memberOf"]
      val groups =
          if (memberOf is Array<*>) {
            val list = memberOf.toList()
            parseGroups(list.filterIsInstance<String>())
          } else {
            emptyList()
          }
      ADUser(
          sAMAccountName = memberMap["sAMAccountName"].toString(),
          givenName = memberMap["givenName"].toString(),
          sn = memberMap["sn"].toString(),
          displayName = memberMap["displayName"].toString(),
          mail = memberMap["mail"].toString(),
          telephoneNumber = memberMap["telephoneNumber"].toString(),
          employeeID = memberMap["employeeID"].toString(),
          objectGuid = memberMap["objectGUID"].toString(),
          whenCreated = memberMap["whenCreated"].toString(),
          enabled = memberMap["userAccountControl"].toString().toInt() and 2 != 2,
          groups = groups)
    }
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
      val startIndex = GROUP_NAME_PREFIX_LENGTH // to remove "cn=" from the start of the string.
      val cn = parts.first().substring(startIndex)
      val dn = it
      ADGroup(cn, distinguishedName = dn, objectGuid = null)
    } ?: emptyList()
  }
}
