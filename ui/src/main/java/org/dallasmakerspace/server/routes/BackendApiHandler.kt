package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService

class BackendApiHandler(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    val memberService: MemberService,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  override suspend fun handleAuthenticated(call: ApplicationCall) {
    val path = call.request.path().removePrefix("/backend-api/")
    val queryString = call.request.queryString()
    val fullPath = if (queryString.isNotEmpty()) "$path?$queryString" else path
    val result = memberService.callBackendApi(fullPath, session.sessionId)
    call.respond(result)
  }
}
