package org.dallasmakerspace.server.memberservice

import javax.inject.Inject
import org.dallasmakerspace.models.DMSGroup
import org.dallasmakerspace.models.DMSMember
import org.dallasmakerspace.server.common.HttpException
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.models.EventSummary
import org.dallasmakerspace.server.models.SearchPreloadResponse
import org.dallasmakerspace.server.models.getSlugFromName
import org.dallasmakerspace.server.voterregistration.VoterRegistrationManager

class MemberService
@Inject
constructor(
    loggerFactory: LoggerFactory,
    private val memberServiceClient: MemberServiceClient,
    private val votingRegistrationManager: VoterRegistrationManager,
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun linkDiscourseAccount(
      username: String,
      discourseUsername: String,
      discourseAvatarUrl: String?,
      sessionId: String?,
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

  suspend fun linkDiscordAccount(
      username: String,
      discordUserId: String,
      discordUsername: String,
      discordAvatarUrl: String?,
      sessionId: String?,
  ) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Update Discord fields
    member.discordUserId = discordUserId
    member.discordUsername = discordUsername
    member.discordAvatarUrl = discordAvatarUrl
    try { // Patch object
      memberServiceClient.patchMember(username, member, sessionId)
    } catch (e: HttpException) {
      log.error("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun unlinkDiscordAccount(username: String, sessionId: String?) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Clear Discord fields
    member.discordUserId = null
    member.discordUsername = null
    member.discordAvatarUrl = null
    try { // Patch object
      memberServiceClient.patchMember(username, member, sessionId)
    } catch (e: HttpException) {
      log.error("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun linkLinkedInAccount(username: String, linkedinUsername: String, sessionId: String?) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Update LinkedIn field
    member.linkedinUsername = linkedinUsername
    try { // Patch object
      memberServiceClient.patchMember(username, member, sessionId)
    } catch (e: HttpException) {
      log.error("Failed to patch member: $username; member object: $member", e)
      throw MemberServiceException("Failed to patch member: $username; member object: $member", e)
    }
  }

  suspend fun unlinkLinkedInAccount(username: String, sessionId: String?) {
    // Get member object
    val member = memberServiceClient.getMember(username, sessionId)
    // Clear LinkedIn field
    member.linkedinUsername = null
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

  suspend fun getSearchPreload(sessionId: String?): SearchPreloadResponse {
    return memberServiceClient.getSearchPreloads(sessionId)
  }

  suspend fun registerVoting(sessionId: String?, username: String) {
    memberServiceClient.addToGroup(
        sessionId,
        username,
        getSlugFromName(votingRegistrationManager.getVotingMembersGroupName()),
    )
  }

  suspend fun unregisterVoting(sessionId: String?, username: String) {
    memberServiceClient.removeFromGroup(
        sessionId,
        username,
        getSlugFromName(votingRegistrationManager.getVotingMembersGroupName()),
    )
  }

  suspend fun callBackendApi(path: String, sessionId: String?) =
      memberServiceClient.callBackendApi(path, sessionId)

  suspend fun callBackendApiPost(path: String, sessionId: String?, username: String?, body: Any) =
      memberServiceClient.callBackendApiPost(path, sessionId, username, body)

  suspend fun callBackendApiPatch(path: String, sessionId: String?, username: String?, body: Any) =
      memberServiceClient.callBackendApiPatch(path, sessionId, username, body)

  suspend fun callBackendApiDelete(path: String, sessionId: String?, username: String?) =
      memberServiceClient.callBackendApiDelete(path, sessionId, username)

  suspend fun getEventsOrganizedByMember(
      username: String,
      limit: Int,
      sessionId: String?,
  ): List<EventSummary> {
    return memberServiceClient.getEventsOrganizedByMember(username, limit, sessionId)
  }

  suspend fun hasPrerequisiteClasses(groupSlug: String, sessionId: String?): Boolean {
    return memberServiceClient.hasPrerequisiteClasses(groupSlug, sessionId)
  }

  suspend fun askAi(
      sessionId: String?,
      question: String,
      username: String?,
      forceRefresh: Boolean = false,
  ): Map<String, Any?> {
    return memberServiceClient.askAi(sessionId, question, username, forceRefresh)
  }

  suspend fun getAskAiTopQuestions(sessionId: String?, limit: Int = 10): List<Map<String, Any?>> {
    return memberServiceClient.getAskAiTopQuestions(sessionId, limit)
  }

  suspend fun getAskAiBySlug(sessionId: String?, slug: String): Map<String, Any?>? {
    return memberServiceClient.getAskAiBySlug(sessionId, slug)
  }

  suspend fun submitAskAiFeedback(sessionId: String?, cacheId: Int, isHelpful: Boolean): Boolean {
    return memberServiceClient.submitAskAiFeedback(sessionId, cacheId, isHelpful)
  }
}
