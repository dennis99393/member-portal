package org.dallasmakerspace.activedirectory

import com.unboundid.ldap.sdk.FailoverServerSet
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SimpleBindRequest
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

  override fun getUser(username: String): Map<String, Any?> {
    val filter =
        Filter.createANDFilter(
            listOf(
                Filter.createEqualityFilter("objectCategory", "person"),
                Filter.createORFilter(
                    listOf(
                        Filter.createEqualityFilter("objectClass", "member"),
                        Filter.createEqualityFilter("objectClass", "user"))),
                Filter.createEqualityFilter("sAMAccountName", username)))
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
    val user = searchResult.searchEntries.firstOrNull()
    return user?.attributes?.associate {
      it.name to if (it.name == "memberOf") it.values else it.values?.firstOrNull()
    } ?: throw ADException("User $username not found in AD")
  }
}
