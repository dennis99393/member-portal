package org.dallasmakerspace.thymeleaf.server.memberservice

import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.common.HttpException
import org.dallasmakerspace.thymeleaf.server.common.Log
import org.dallasmakerspace.thymeleaf.server.models.DMSMember

class MemberService @Inject constructor(private val memberServiceClient: MemberServiceClient) {
  suspend fun linkDiscourseAccount(
      username: String,
      discourseUsername: String,
      discourseAvatarUrl: String?
  ) {
    // Get member object
    val member = memberServiceClient.getMember(username)
    // Update discourseUserName
    member.discourseUsername = discourseUsername
    member.discourseAvatarUrl = discourseAvatarUrl
    try { // Patch object
      memberServiceClient.patchMember(username, member)
    } catch (e: HttpException) {
      Log.e("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun getMember(username: String): DMSMember {
    return memberServiceClient.getMember(username)
  }

  suspend fun unlinkDiscourseAccount(username: String) {
    // Get member object
    val member = memberServiceClient.getMember(username)
    // Update discourseUserName
    member.discourseUsername = null
    member.discourseAvatarUrl = null
    try { // Patch object
      memberServiceClient.patchMember(username, member)
    } catch (e: HttpException) {
      Log.e("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }
}
