package org.dallasmakerspace.activedirectory

class ActiveDirectoryClientMock : IActiveDirectoryClient {
  override fun getUsersByUsernameList(usernames: List<String>): Map<String, Map<String, Any?>> =
      emptyMap()

  override fun getGroup(groupname: String): Map<String, Any?> = emptyMap()

  override fun getUsersByDnList(dnList: List<String>): Map<String, Map<String, Any?>> = emptyMap()

  override fun getUsersByLogonDays(days: Int): Map<String, Map<String, Any?>> = emptyMap()
}
