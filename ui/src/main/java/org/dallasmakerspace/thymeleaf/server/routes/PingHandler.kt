package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService

class PingHandler
@Inject
constructor(loggerFactory: LoggerFactory, private val memberService: MemberService) :
    IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  @Suppress("TooGenericExceptionCaught")
  override suspend fun handle(call: ApplicationCall) {
    log.info("PingHandler start")
    val startTime = System.currentTimeMillis()
    try {
      memberService.getMember("test", "ping")
    } catch (e: Exception) {
      val timeTaken = System.currentTimeMillis() - startTime
      call.respond(
          HttpStatusCode.InternalServerError,
          "Member service error: ${e.message}; ${e.cause?.message}\nMember service time taken: $timeTaken")
    }
    val timeTaken = System.currentTimeMillis() - startTime
    call.respond("pong\nMember service time taken: $timeTaken")
  }
}
