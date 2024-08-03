package org.dallasmakerspace.activedirectory

interface IActiveDirectoryClient {
  fun getUsers(usernames: List<String>): Map<String, Map<String, Any?>>
}
