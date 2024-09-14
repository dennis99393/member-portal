package org.dallasmakerspace.activedirectory

interface IActiveDirectoryService {

  /**
   * Gets the ADUser object for the given username.
   *
   * @param username The username of the user to get.
   * @return The ADUser object for the given username.
   */
  fun getMemberByUsername(username: String): ADUser

  /**
   * Gets the ADUser object for the given list of usernames.
   *
   * @param usernameList The list of usernames to get the ADUser object for.
   * @return The map of ADUser objects for the given usernames.
   */
  fun getMembersByUsernameList(usernameList: List<String>): Map<String, ADUser?>

  /** Gets the ADGroup object for the given groupname. */
  fun getGroup(groupname: String): ADGroup

  /**
   * Gets the ADUser objects for the given list of distinguished names.
   *
   * @param dnList The list of distinguished names to get the ADUser objects for. The format of the
   *   distinguished name is "CN=John Doe,OU=Users,DC=example,DC=com".
   */
  fun getMembersByDnList(dnList: List<String>): List<ADUser>

  /** Gets the users that have been logged in the last `days` days. */
  fun getMembersByLoggedInDays(days: Int): List<ADUser>
}
