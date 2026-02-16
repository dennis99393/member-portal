package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.models.Committees
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.models.getSlugFromName

class PingHandler
@Inject
constructor(loggerFactory: LoggerFactory, private val memberService: MemberService) :
    IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handle(call: ApplicationCall) {
    log.info("PingHandler start")
    val startTime = System.currentTimeMillis()

    // Fetch member profile
    try {
      memberService.getMember("test", "ping")
    } catch (e: Exception) {
      val timeTaken = System.currentTimeMillis() - startTime
      call.respond(
          HttpStatusCode.InternalServerError,
          "Member service error: ${e.message}; ${e.cause?.message}\nMember service time taken: $timeTaken")
      return
    }
    val memberTimeTaken = System.currentTimeMillis() - startTime

    // Fetch random group from committee teacher groups
    val groupStartTime = System.currentTimeMillis()
    val allTeacherGroups =
        Committees.getActiveCommittees().flatMap { it.teacherGroups }.filter { it.isNotEmpty() }
    val randomGroup = allTeacherGroups.random()
    val groupSlug = getSlugFromName(randomGroup)
    var groupTimeTaken: Long = 0
    var groupError: String? = null

    try {
      memberService.getGroup(groupSlug, "ping")
      groupTimeTaken = System.currentTimeMillis() - groupStartTime
    } catch (e: Exception) {
      groupTimeTaken = System.currentTimeMillis() - groupStartTime
      groupError = "${e.message}; ${e.cause?.message}"
    }

    val totalTimeTaken = System.currentTimeMillis() - startTime
    val response = buildString {
      appendLine("pong")
      appendLine("Member service time taken: $memberTimeTaken ms")
      appendLine("Group fetch ($randomGroup): $groupTimeTaken ms")
      if (groupError != null) {
        appendLine("Group fetch error: $groupError")
      }
      append("Total time taken: $totalTimeTaken ms")
    }

    call.respond(response)
  }
}
