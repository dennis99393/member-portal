package org.dallasmakerspace.discourse

interface IDiscourseApiClient {
  suspend fun addUsersToGroup(memberUsernamesList: List<String>, groupId: DiscourseService.GroupId)

  suspend fun removeMembersFromGroup(
      memberUsernamesList: List<String>,
      groupId: DiscourseService.GroupId
  )

  suspend fun getUserProfile(username: String): DiscourseUserProfile
}
