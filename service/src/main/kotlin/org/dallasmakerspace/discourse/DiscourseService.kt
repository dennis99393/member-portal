package org.dallasmakerspace.discourse

import javax.inject.Inject

class DiscourseService @Inject constructor(private val discourseApiClient: IDiscourseApiClient) {

  /**
   * Adds a discourse user to a group
   *
   * @param username the username of the user to add to the group
   * @param groupId the id of the group to add the user to, must be a value from the GroupId enum
   */
  private suspend fun addUserToGroup(username: String, groupId: GroupId) {
    discourseApiClient.addUserToGroup(username, groupId)
  }

  /**
   * Adds a user to the DMS Members V2 group
   *
   * @param username the username of the user to add to the group
   */
  suspend fun addUserToDmsMembersV2Group(username: String) =
      addUserToGroup(username, GroupId.GROUP_DMS_MEMBERS_V2)

  /**
   * Removes a discourse user from a group
   *
   * @param username the username of the user to remove from the group
   * @param groupId the id of the group to remove the user from, must be a value from the GroupId
   *   enum
   */
  private suspend fun removeUserFromGroup(username: String, groupId: GroupId) {
    discourseApiClient.removeMemberFromGroup(username, groupId)
  }

  /**
   * Adds a user to the DMS Members V2 group
   *
   * @param username the username of the user to add to the group
   */
  suspend fun removeUserFromDmsMembersV2Group(username: String) =
      removeUserFromGroup(username, GroupId.GROUP_DMS_MEMBERS_V2)

  // Enum to store group ids
  enum class GroupId(val id: Int) {
    GROUP_DMS_MEMBERS_V2(DISCOURSE_GROUP_DMS_MEMBERS_V2_ID)
  }

  private companion object {
    const val DISCOURSE_GROUP_DMS_MEMBERS_V2_ID = 109
  }
}
