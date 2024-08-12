package org.dallasmakerspace.activedirectory

interface IActiveDirectoryClient {
  fun getUsersByUsernameList(usernames: List<String>): Map<String, Map<String, Any?>>

  fun getGroup(groupname: String): Map<String, Any?>

  /**
   * Gets the user objects for the given list of distinguished names.
   *
   * @param dnList The list of distinguished names to get the user objects for. The format of the
   *   distinguished name is "CN=John Doe,OU=Users,DC=example,DC=com".
   */
  fun getUsersByDnList(dnList: List<String>): Map<String, Map<String, Any?>>
}
