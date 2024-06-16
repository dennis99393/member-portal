package org.dallasmakerspace.activedirectory

interface IActiveDirectoryService {
  /**
   * Gets the ADUser object for the given username.
   *
   * @param username The username of the user to get.
   * @return The ADUser object for the given username.
   */
  fun getMember(username: String): ADUser
}
