package org.dallasmakerspace.activedirectory

class ActiveDirectoryClientMock : IActiveDirectoryClient {
  override fun getUsers(usernames: List<String>): Map<String, Map<String, Any?>> = emptyMap()
}
