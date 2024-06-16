package org.dallasmakerspace.activedirectory

interface IActiveDirectoryClient {
  fun getUser(username: String): Map<String, Any?>
}
