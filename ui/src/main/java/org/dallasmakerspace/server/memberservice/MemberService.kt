package org.dallasmakerspace.server.memberservice

import javax.inject.Inject
import org.dallasmakerspace.server.common.HttpException
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.models.DMSGroup
import org.dallasmakerspace.server.models.DMSMember

class MemberService
@Inject
constructor(loggerFactory: LoggerFactory, private val memberServiceClient: MemberServiceClient) {
  private val log = loggerFactory.create(javaClass)

  suspend fun linkDiscourseAccount(
      username: String,
      discourseUsername: String,
      discourseAvatarUrl: String?,
      sessionId: String?
  ) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Update discourseUserName
    member.discourseUsername = discourseUsername
    member.discourseAvatarUrl = discourseAvatarUrl
    try { // Patch object
      memberServiceClient.patchMember(username, member, sessionId)
    } catch (e: HttpException) {
      log.error("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun getMember(username: String, sessionId: String?): DMSMember {
    return memberServiceClient.getMember(username, sessionId)
  }

  suspend fun unlinkDiscourseAccount(username: String, sessionId: String?) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Update discourseUserName
    member.discourseUsername = null
    member.discourseAvatarUrl = null
    try { // Patch object
      memberServiceClient.patchMember(username, member, sessionId)
    } catch (e: HttpException) {
      log.error("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun getGroup(groupName: String, sessionId: String?): DMSGroup {
    return memberServiceClient.getGroup(groupName, sessionId)
  }

  suspend fun getSearchPreload(sessionId: String?): List<DMSMember> {
    return memberServiceClient.getSearchPreloads(sessionId)
  }
}
