package org.dallasmakerspace.activedirectory

import dagger.Reusable
import javax.inject.Inject

private const val GROUP_NAME_PREFIX_LENGTH = 3
private const val MAX_GROUP_MEMBERS = 20

@Reusable
class ActiveDirectoryService
@Inject
constructor(private val activeDirectoryClient: IActiveDirectoryClient) : IActiveDirectoryService {

  /** {@inheritDoc} */
  override fun getMemberByUsernameList(username: String): ADUser {
    return getMembersByUsernameList(listOf(username))[username]
        ?: throw ADException("User $username not found in AD")
  }

  /** {@inheritDoc} */
  override fun getMemberByDnList(dnList: List<String>): List<ADUser> {
    val adSearchResult: Map<String, Map<String, Any?>> =
        activeDirectoryClient.getUsersByDnList(dnList)

    // For each DN in the list, get the ADUser object.
    return dnList
        .map { dn ->
          val memberMap = adSearchResult[dn] ?: return@map null
          val groups = emptyList<ADGroup>()
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
        .filterNotNull()
  }

  /** {@inheritDoc} */
  override fun getMembersByUsernameList(usernameList: List<String>): Map<String, ADUser?> {
    val adSearchResult: Map<String, Map<String, Any?>> =
        activeDirectoryClient.getUsersByUsernameList(usernameList)

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

  /** {@inheritDoc} */
  override fun getGroup(groupname: String): ADGroup {
    val adResult = activeDirectoryClient.getGroup(groupname)
    return ADGroup(
        cn = adResult["cn"].toString(),
        description = adResult["description"]?.toString(),
        distinguishedName = adResult["distinguishedName"].toString(),
        objectGuid = adResult["objectGUID"]?.toString(), // Convert bytes to GUID String
        members = parseMembers(adResult),
        membersListIncomplete =
            (adResult["member;range=0-1499"] != null &&
                (adResult["member;range=0-1499"] as Array<*>).isNotEmpty()) ||
                (adResult["member"] != null &&
                    (adResult["member"] as Array<*>).size > MAX_GROUP_MEMBERS))
  }

  /** {@inheritDoc} */
  override fun getMembersByUpdatedDays(days: Int): List<ADUser> {
    val adSearchResult: Map<String, Map<String, Any?>> =
        activeDirectoryClient.getUsersByLogonDays(days)

    // For each username in the list, get the ADUser object.
    return adSearchResult.values.map { memberMap ->
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
   * Parses the list of members into a list of ADUser objects.
   *
   * @param attributeMap The map of attributes for the group. The members of the group are in either
   *   "member" or "member;range=0-1499" attribute. Each member is in distinguishedName format
   *   e.g. - "CN=John Doe1337,OU=Members,DC=dms,DC=local".
   *     @return The list of ADUser objects. Empty list if the input list is null.
   */
  private fun parseMembers(attributeMap: Map<String, Any?>): List<ADUser> {
    // Get the list of members from the attribute map "member" or "member;range=0-1499" attribute.
    val allMembers =
        if (attributeMap["member;range=0-1499"] != null &&
            (attributeMap["member;range=0-1499"] as Array<*>).isNotEmpty()) {
          attributeMap["member;range=0-1499"]
        } else {
          attributeMap["member"]
        }
    // Take first [MAX_GROUP_MEMBERS] members from the list.
    val members = (allMembers as Array<*>).take(MAX_GROUP_MEMBERS)
    // Fetch the ADUser object for each member.
    return getMemberByDnList(members.map { it.toString() })
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
      ADGroup(
          cn,
          distinguishedName = dn,
          description = null,
          objectGuid = null,
          members = listOf(),
          membersListIncomplete = false,
      )
    } ?: emptyList()
  }
}
