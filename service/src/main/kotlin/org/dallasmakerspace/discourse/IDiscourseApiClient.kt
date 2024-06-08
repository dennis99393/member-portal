package org.dallasmakerspace.discourse

interface IDiscourseApiClient {
  suspend fun addUserToGroup(memberUsername: String, groupId: DiscourseService.GroupId)

  suspend fun removeMemberFromGroup(memberUsername: String, groupId: DiscourseService.GroupId)
}
