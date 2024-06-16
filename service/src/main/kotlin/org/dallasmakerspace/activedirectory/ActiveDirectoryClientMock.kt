package org.dallasmakerspace.activedirectory

class ActiveDirectoryClientMock : IActiveDirectoryClient {
  override fun getUser(username: String): Map<String, Any?> = emptyMap()
}
