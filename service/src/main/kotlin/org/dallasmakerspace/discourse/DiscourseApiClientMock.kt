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

  override suspend fun getUserProfile(username: String): DiscourseUserProfile {
    log.info("Mocked fetching user profile for username: $username")
    return DiscourseUserProfile(
        user =
            DiscourseUser(
                id = 12345,
                username = username,
                name = "Mock User",
                avatarTemplate =
                    "/user_avatar/talk.dallasmakerspace.org/$username/{size}/123_2.png",
                title = "Mock Title",
                admin = false,
                moderator = false,
                trustLevel = 1))
  }
}
