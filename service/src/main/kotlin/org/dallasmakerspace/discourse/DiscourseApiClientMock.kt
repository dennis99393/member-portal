package org.dallasmakerspace.discourse

import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.LoggerFactory

/** Mock implementation of the DiscourseApiClient for testing purposes. */
@Singleton
class DiscourseApiClientMock @Inject constructor(loggerFactory: LoggerFactory) :
    IDiscourseApiClient {
  private val log = loggerFactory.create(javaClass)

  override suspend fun addUsersToGroup(
      memberUsernamesList: List<String>,
      groupId: DiscourseService.GroupId
  ) {
    log.info("Mocked adding member(s) $memberUsernamesList to group ${groupId.name}")
    // throw DiscourseApiException("DiscourseApiClientMock: Failed to add member")
    return
  }

  override suspend fun removeMembersFromGroup(
      memberUsernamesList: List<String>,
      groupId: DiscourseService.GroupId
  ) {
    log.info("Mocked removing member(s) $memberUsernamesList to group ${groupId.name}")
    // throw DiscourseApiException("DiscourseApiClientMock: Failed to remove member")
    return
  }
}
