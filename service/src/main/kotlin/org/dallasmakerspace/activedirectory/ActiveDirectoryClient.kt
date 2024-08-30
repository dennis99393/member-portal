package org.dallasmakerspace.activedirectory

import com.unboundid.ldap.sdk.FailoverServerSet
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SimpleBindRequest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig

private const val INITIAL_LDAP_CONNECTIONS = 3

private const val MAX_LDAP_CONNECTIONS = 5

@Singleton
class ActiveDirectoryClient @Inject constructor(appConfig: AppConfig) : IActiveDirectoryClient {
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
          MAX_LDAP_CONNECTIONS)

  override fun getUsersByUsernameList(usernames: List<String>): Map<String, Map<String, Any?>> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createORFilter(
                    listOf(
                        Filter.createEqualityFilter("objectClass", "member"),
                        Filter.createEqualityFilter("objectClass", "user"))),
                Filter.createORFilter(
                    usernames.map {
                      Filter.createEqualityFilter(
                          "sAMAccountName",
                          it,
                      )
                    })))
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
            "whenCreated")
    return searchResult.searchEntries.associate { entry ->
      val username = entry.getAttributeValue("sAMAccountName")

      username to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }

  override fun getGroup(groupname: String): Map<String, Any?> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("name", groupname),
                Filter.createEqualityFilter("objectCategory", "group")))
    val searchResult =
        ldapPool.search(
            "DC=dms, DC=local",
            SearchScope.SUB,
            filter,
            "cn",
            "distinguishedName",
            "description",
            "member",
            "objectGUID")
    val group =
        searchResult.searchEntries.firstOrNull()?.attributes?.associate {
          if (it.name.startsWith("member")) {
            it.name to it.values
          } else it.name to it.values.firstOrNull()
        } ?: throw ADException("Group $groupname not found in AD")

    return group
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
                        Filter.createEqualityFilter("objectClass", "user"))),
                Filter.createORFilter(
                    dnList.map {
                      Filter.createEqualityFilter(
                          "distinguishedName",
                          it,
                      )
                    })))
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
            "whenCreated")
    return searchResult.searchEntries.associate { entry ->
      val dn = entry.dn.toString()

      dn to
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

    // Construct the LDAP filter to search for users who logged in or account was updated the last N
    // days
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createEqualityFilter("objectClass", "person"),
                Filter.createGreaterOrEqualFilter(
                    "lastLogonTimestamp", dateNDaysAgoInLdapFormat.toString())))

    // Perform the search
    val searchResult =
        ldapPool.search(
            "ou=Members,dc=dms,dc=local",
            SearchScope.SUB,
            filter,
            "sAMAccountName",
            "givenName",
            "sn",
            "displayName",
            "userAccountControl",
            "whenCreated")

    // Process the search results
    return searchResult.searchEntries.associate { entry ->
      val preferredUsername = entry.getAttributeValue("sAMAccountName")

      preferredUsername to
          entry.attributes.associate {
            it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
          }
    }
  }
}
