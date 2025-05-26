package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService

class BackendApiHandler(
    loggerFactory: LoggerFactory,
    val memberService: MemberService,
    userInfoProvider: UserInfoProvider,
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    val path = call.request.path().removePrefix("/backend-api/")
    val result = memberService.callBackendApi(path, session?.sessionId)
    call.respond(result)
  }
}
