package org.dallasmakerspace.activedirectory

import com.unboundid.ldap.sdk.FailoverServerSet
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchRequest.ALL_USER_ATTRIBUTES
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SimpleBindRequest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

private const val INITIAL_LDAP_CONNECTIONS = 3

private const val MAX_LDAP_CONNECTIONS = 5

@Singleton
class ActiveDirectoryClient
@Inject
constructor(appConfig: AppConfig, loggerFactory: LoggerFactory) : IActiveDirectoryClient {
  private val log = loggerFactory.create(javaClass)

  private val ldapUser = appConfig.requireStringProperty("app.ldap.user")
  private val ldapPass = appConfig.requireStringProperty("app.ldap.password")
  private val bindHost = appConfig.requireStringProperty("app.ldap.host")
  private val bindPort = appConfig.requireIntProperty("app.ldap.port")
  private val serverSet = FailoverServerSet(arrayOf(bindHost), intArrayOf(bindPort))
  private val ldapPool =
      LDAPConnectionPool(
          serverSet,
          SimpleBindRequest(ldapUser, ldapPass),
          INITIAL_LDAP_CONNECTIONS,
          MAX_LDAP_CONNECTIONS,
      )

  override fun getUsersByUsernameList(usernames: List<String>): Map<String, Map<String, Any?>> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createORFilter(
                    listOf(
                        Filter.createEqualityFilter("objectClass", "member"),
                        Filter.createEqualityFilter("objectClass", "user"),
                    )),
                Filter.createORFilter(
                    usernames.map {
                      Filter.createEqualityFilter(
                          "sAMAccountName",
                          it,
                      )
                    }),
            ))
    val searchResult =
        ldapPool.search(
            "DC=dms, DC=local",
            SearchScope.SUB,
            filter,
            "sAMAccountName",
            "givenName",
            "sn",
            "displayName",
            "mail",
            "memberOf",
            "employeeID",
            "telephoneNumber",
            "objectGUID",
            "userAccountControl",
            "whenCreated",
        )
    return searchResult.searchEntries.associate { entry ->
      val username = entry.getAttributeValue("sAMAccountName")

      username to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }

  override fun getGroup(groupname: String): Map<String, Any?> {
    val groupFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("name", groupname),
                Filter.createEqualityFilter("objectCategory", "group"),
            ))

    val searchResult =
        ldapPool.search(
            "DC=dms,DC=local",
            SearchScope.SUB,
            groupFilter,
            "cn",
            "distinguishedName",
            "description",
            "member",
            "objectGUID",
            "managedBy",
            "nTSecurityDescriptor",
        )

    val groupEntry =
        searchResult.searchEntries.firstOrNull()
            ?: throw ADException("Group $groupname not found in AD")

    val group =
        groupEntry.attributes.associate {
          it.name to if (it.name == "member") it.values else it.values.firstOrNull()
        }

    // Escape special characters like '(' and ')' in the group DN
    val groupDN =
        (group["distinguishedName"] as String?)?.replace("(", "\\28")?.replace(")", "\\29")
            ?: throw ADException("Group DN not found")

    // Get all members with paging to handle large groups
    val allMembers = getAllGroupMembers(groupDN)

    // Get group administrators - entities that can modify the group
    val groupAdmins = getGroupAdministrators(groupEntry)

    // Update the group information
    val updatedGroup = group.toMutableMap()
    updatedGroup["member"] = allMembers.toTypedArray()
    updatedGroup["administrators"] = groupAdmins.toTypedArray()

    return updatedGroup
  }

  /**
   * Retrieves all members of a group, handling pagination for large groups.
   *
   * @param groupDN The distinguished name of the group
   * @return List of distinguished names of all members
   */
  private fun getAllGroupMembers(groupDN: String): List<String> {
    val allMembers = mutableListOf<String>()

    // Query the group directly using its DN to get all members
    // The correct way to retrieve large groups is to query the group's member attribute with range
    // retrieval
    val groupFilter = Filter.createEqualityFilter("distinguishedName", groupDN)

    // Use a range to retrieve all members in batches
    var rangeStart = 0
    val rangeSize = 1000
    var hasMoreMembers = true

    while (hasMoreMembers) {
      val rangeEnd = if (rangeStart == 0) rangeSize - 1 else "*"
      val attributeName = "member;range=$rangeStart-$rangeEnd"

      val searchRequest =
          SearchRequest("DC=dms,DC=local", SearchScope.SUB, groupFilter, attributeName)

      val searchResult = ldapPool.search(searchRequest)

      if (searchResult.searchEntries.isEmpty()) {
        break
      }

      val entry = searchResult.searchEntries.first()
      val rangeAttribute = entry.attributes.find { it.name.startsWith("member;range=") }

      if (rangeAttribute != null) {
        val rangeValues = rangeAttribute.values
        if (rangeValues != null && rangeValues.isNotEmpty()) {
          allMembers.addAll(rangeValues)
        }

        // Check if we need to continue retrieving more members
        if (rangeAttribute.name.endsWith("-*")) {
          hasMoreMembers = false
        } else {
          rangeStart += rangeSize
        }
      } else {
        // If no range attribute is found but there's a standard member attribute, use that
        val memberAttribute = entry.getAttribute("member")
        if (memberAttribute != null && memberAttribute.values != null) {
          allMembers.addAll(memberAttribute.values)
        }
        hasMoreMembers = false
      }
    }

    return allMembers
  }

  /**
   * Retrieves the list of users/groups that have permissions to modify the group.
   *
   * @param groupEntry The LDAP entry for the group
   * @return List of distinguished names of administrators
   */
  private fun getGroupAdministrators(groupEntry: com.unboundid.ldap.sdk.Entry): List<String> {
    val administrators = mutableListOf<String>()

    // First check managedBy attribute
    val managedBy = groupEntry.getAttributeValue("managedBy")
    if (!managedBy.isNullOrEmpty()) {
      administrators.add(managedBy)
    }

    // Check nTSecurityDescriptor to find who has permissions to modify the group
    // This is a simplified approach since full security descriptor parsing is complex

    // Add domain admins (they always have permission)
    val domainAdminsFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("name", "Domain Admins"),
                Filter.createEqualityFilter("objectCategory", "group"),
            ))

    val domainAdminsResult =
        ldapPool.search("DC=dms,DC=local", SearchScope.SUB, domainAdminsFilter, "distinguishedName")

    val domainAdminsDN = domainAdminsResult.searchEntries.firstOrNull()?.dn?.toString()
    if (!domainAdminsDN.isNullOrEmpty()) {
      administrators.add(domainAdminsDN)
    }

    return administrators
  }

  /** {@inheritDoc} */
  override fun getUsersByDnList(dnList: List<String>): Map<String, Map<String, Any?>> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createORFilter(
                    listOf(
                        Filter.createEqualityFilter("objectClass", "member"),
                        Filter.createEqualityFilter("objectClass", "user"),
                    )),
                Filter.createORFilter(
                    dnList.map {
                      Filter.createEqualityFilter(
                          "distinguishedName",
                          it,
                      )
                    }),
            ))
    val searchResult =
        ldapPool.search(
            "DC=dms, DC=local",
            SearchScope.SUB,
            filter,
            "sAMAccountName",
            "givenName",
            "sn",
            "displayName",
            "mail",
            "memberOf",
            "employeeID",
            "telephoneNumber",
            "objectGUID",
            "userAccountControl",
            "whenCreated",
        )
    return searchResult.searchEntries.associate { entry ->
      val dn = entry.dn.toString()

      dn to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }

  override fun getUsersByBadgeNumberList(
      badgeNumberList: List<String>
  ): Map<String, Map<String, Any?>> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createORFilter(
                    listOf(
                        Filter.createEqualityFilter("objectClass", "member"),
                        Filter.createEqualityFilter("objectClass", "user"),
                    )),
                Filter.createORFilter(
                    badgeNumberList.map {
                      Filter.createEqualityFilter(
                          "employeeID",
                          it,
                      )
                    }),
            ))
    val searchResult =
        ldapPool.search(
            "DC=dms, DC=local",
            SearchScope.SUB,
            filter,
            "sAMAccountName",
            "givenName",
            "sn",
            "displayName",
            "mail",
            "memberOf",
            "employeeID",
            "telephoneNumber",
            "objectGUID",
            "userAccountControl",
            "whenCreated",
        )
    return searchResult.searchEntries.associate { entry ->
      val badgeNumber = entry.getAttributeValue("employeeID")

      badgeNumber to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }

  /** {@inheritDoc} */
  @Suppress("MagicNumber")
  override fun getUsersByLogonDays(days: Int): Map<String, Map<String, Any?>> {
    // Calculate the date N days ago
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, -days)
    val dateNDaysAgo = calendar.time

    // Convert the date to the number format used in LDAP the number for 100-nanosecond intervals
    // since 1601-01-01
    // For e.g. 133688140853991409 will represent 8/22/2024 10:28:05 AM CDT
    val dateNDaysAgoInLdapFormat = (dateNDaysAgo.time + 11644473600000) * 10000

    // Construct the LDAP filter to search for users who logged in the last N days
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createEqualityFilter("objectClass", "person"),
                Filter.createGreaterOrEqualFilter(
                    "lastLogonTimestamp",
                    dateNDaysAgoInLdapFormat.toString(),
                ),
            ))

    // Perform the search
    val searchResult =
        ldapPool.search(
            "ou=Members,dc=dms,dc=local",
            SearchScope.SUB,
            filter,
            "sAMAccountName",
            "givenName",
            "sn",
            "mail",
            "employeeID",
            "telephoneNumber",
            "displayName",
            "userAccountControl",
            "whenCreated",
        )

    // Process the search results
    return searchResult.searchEntries.associate { entry ->
      val preferredUsername = entry.getAttributeValue("sAMAccountName")

      preferredUsername to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }

  /** {@inheritDoc} */
  override fun removeUsersFromGroup(dmsUsernames: List<String>, group: String) {
    val groupFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("name", group),
                Filter.createEqualityFilter("objectCategory", "group"),
            ))

    val searchResult =
        ldapPool.search(
            "DC=dms,DC=local",
            SearchScope.SUB,
            groupFilter,
            "cn",
            "distinguishedName",
            "description",
            "member",
            "objectGUID",
        )

    val groupEntry =
        searchResult.searchEntries.firstOrNull()
            ?: throw ADException("Group $group not found in AD")

    val groupDN = groupEntry.dn.toString()

    val userFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createEqualityFilter("objectClass", "user"),
                Filter.createEqualityFilter("memberOf", groupDN),
                Filter.createORFilter(
                    dmsUsernames.map { Filter.createEqualityFilter("sAMAccountName", it) }),
            ))

    val searchRequest =
        SearchRequest(
            "DC=dms,DC=local",
            SearchScope.SUB,
            userFilter,
            ALL_USER_ATTRIBUTES,
        )

    val userSearchResult = ldapPool.search(searchRequest)

    val usersToRemove =
        userSearchResult.searchEntries.filter {
          it.getAttributeValue("sAMAccountName") in dmsUsernames
        }

    usersToRemove.forEach { user ->
      val userDN = user.dn.toString()
      val mod = Modification(ModificationType.DELETE, "member", userDN)
      ldapPool.modify(groupDN, mod)
    }
  }

  /** {@inheritDoc} */
  override fun addUsersToGroup(dmsUsernames: List<String>, group: String) {
    val groupFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("name", group),
                Filter.createEqualityFilter("objectCategory", "group"),
            ))

    val searchResult =
        ldapPool.search(
            "DC=dms,DC=local",
            SearchScope.SUB,
            groupFilter,
            "cn",
            "distinguishedName",
            "description",
            "member",
            "objectGUID",
        )

    val groupEntry =
        searchResult.searchEntries.firstOrNull()
            ?: throw ADException("Group $group not found in AD")

    val groupDN = groupEntry.dn.toString()

    val userFilter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createEqualityFilter("objectClass", "user"),
                Filter.createORFilter(
                    dmsUsernames.map { Filter.createEqualityFilter("sAMAccountName", it) }),
            ))

    val searchRequest =
        SearchRequest(
            "DC=dms,DC=local",
            SearchScope.SUB,
            userFilter,
            ALL_USER_ATTRIBUTES,
        )

    val userSearchResult = ldapPool.search(searchRequest)

    val usersToAdd =
        userSearchResult.searchEntries.filter {
          it.getAttributeValue("sAMAccountName") in dmsUsernames
        }

    if (usersToAdd.size != dmsUsernames.size) {
      val missingUsers =
          dmsUsernames.filter { username ->
            usersToAdd.none { it.getAttributeValue("sAMAccountName") == username }
          }
      log.error("$TAG/addUsersToGroup/ Users not found in AD: $missingUsers")
    }

    usersToAdd.forEach { user ->
      val userDN = user.dn.toString()
      val mod = Modification(ModificationType.ADD, "member", userDN)
      ldapPool.modify(groupDN, mod)
    }
  }

  /** {@inheritDoc} */
  override fun getAllGroups(): Map<String, Map<String, Any?>> {
    val slowThresholdMs = 1_000L
    val methodStart = System.nanoTime()

    val groupFilter = Filter.createEqualityFilter("objectCategory", "group")

    val searchStart = System.nanoTime()
    val searchResult =
        ldapPool.search(
            "OU=Groups,DC=dms,DC=local",
            SearchScope.SUB,
            groupFilter,
            "cn",
            "distinguishedName",
            "description",
            "objectGUID",
            "member",
        )
    val searchDurationMs = (System.nanoTime() - searchStart) / 1_000_000
    val entryCount = searchResult.searchEntries.size
    if (searchDurationMs > slowThresholdMs) {
      log.warn("$TAG/getAllGroups LDAP search took ${searchDurationMs}ms for ${entryCount} entries")
    } else {
      log.debug(
          "$TAG/getAllGroups LDAP search took ${searchDurationMs}ms for ${entryCount} entries")
    }

    val mapStart = System.nanoTime()
    val result =
        searchResult.searchEntries.associate { groupEntry ->
          val groupName = groupEntry.getAttributeValue("cn")
          val group =
              groupEntry.attributes.associate {
                it.name to
                    if (it.name == "member") {
                      // For member count, just return the count without loading all members
                      it.values?.size ?: 0
                    } else {
                      it.values.firstOrNull()
                    }
              }
          groupName to group
        }
    val mapDurationMs = (System.nanoTime() - mapStart) / 1_000_000

    val totalDurationMs = (System.nanoTime() - methodStart) / 1_000_000
    if (mapDurationMs > slowThresholdMs) {
      log.warn(
          "$TAG/getAllGroups mapping entries took ${mapDurationMs}ms for ${entryCount} entries")
    } else {
      log.debug(
          "$TAG/getAllGroups mapping entries took ${mapDurationMs}ms for ${entryCount} entries")
    }
    if (totalDurationMs > slowThresholdMs) {
      log.warn("$TAG/getAllGroups total duration ${totalDurationMs}ms")
    } else {
      log.debug("$TAG/getAllGroups total duration ${totalDurationMs}ms")
    }

    return result
  }

  companion object {
    private const val TAG = "ActiveDirectoryClient"
  }
}
