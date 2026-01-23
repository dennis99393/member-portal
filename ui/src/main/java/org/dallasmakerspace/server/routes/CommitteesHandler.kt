package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.models.Committee
import org.dallasmakerspace.models.Committees
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.getSlugFromName

class CommitteesHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val committees = Committees.ALL.filter { it.isActive }

    // Fetch all groups once with a single AD call
    val allGroups = try {
      memberService.getAllGroups(session.sessionId)
    } catch (e: Exception) {
      log.warn("Failed to fetch all groups: {}", e.message)
      emptyList()
    }

    // Create a map of group slug -> group for quick lookup
    val groupsBySlug = allGroups.associateBy { getSlugFromName(it.name) }

    // Build committee data using the cached groups
    val committeeData = committees.map { committee -> buildCommitteeMap(committee, groupsBySlug) }

    val jsonMap: MutableMap<String, Any> = mutableMapOf("committees" to committeeData)

    call.respond(ThymeleafContent("committees", jsonMap))
  }

  private suspend fun buildCommitteeMap(committee: Committee): Map<String, Any?> {
    val chairpersons = fetchChairMembers(committee)

    val teacherGroupLinks =
        committee.teacherGroups.map { groupName ->
          mapOf("name" to groupName, "slug" to getSlugFromName(groupName))
        }

    return mutableMapOf(
        "id" to committee.id,
        "name" to committee.name,
        "description" to committee.description,
        "color" to committee.color,
        "chairpersons" to chairpersons,
        "teacherGroups" to teacherGroupLinks,
    )
  }

  private suspend fun fetchChairMembers(committee: Committee): List<Map<String, Any?>> {
    val chairGroupName = committee.chairGroupName ?: return emptyList()

    return try {
      val chairGroupSlug = getSlugFromName(chairGroupName)
      val group = memberService.getGroup(chairGroupSlug, session.sessionId)

      group.members.map { member ->
        val avatarUrl =
            member.avatarUrl
                ?.takeIf { it.isNotEmpty() }
                ?.let { url ->
                  when {
                    url.startsWith("//") -> "https:$url"
                    else -> "https://talk.dallasmakerspace.org$url"
                  }.replace("{size}", "144")
                } ?: ""

        mapOf(
            "username" to member.username,
            "displayName" to member.displayName,
            "avatarUrl" to avatarUrl,
        )
      }
    } catch (e: Exception) {
      log.warn("Failed to fetch chair group for {}: {}", committee.name, e.message)
      emptyList()
    }
  }
}
