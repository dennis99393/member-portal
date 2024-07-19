package org.dallasmakerspace.discourse

import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

/** Mock implementation of the DiscourseApiClient for testing purposes. */
@Singleton
class DiscourseApiClientMock
@Inject
constructor(private val ignored: AppConfig, loggerFactory: LoggerFactory) : IDiscourseApiClient {
  private val log = loggerFactory.create(javaClass)

  override suspend fun addUserToGroup(memberUsername: String, groupId: DiscourseService.GroupId) {
    log.info("Mocked adding member $memberUsername to group ${groupId.name}")
    // throw DiscourseApiException("DiscourseApiClientMock: Failed to add member")
    return
  }

  override suspend fun removeMemberFromGroup(
      memberUsername: String,
      groupId: DiscourseService.GroupId
  ) {
    log.info("Mocked removing member $memberUsername to group ${groupId.name}")
    // throw DiscourseApiException("DiscourseApiClientMock: Failed to remove member")
    return
  }
}
