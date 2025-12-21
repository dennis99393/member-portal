package org.dallasmakerspace.server.routes

import io.ktor.http.*
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
    val username = userInfo["preferred_username"] as? String

    val result =
        when (call.request.httpMethod) {
          HttpMethod.Get -> memberService.callBackendApi(fullPath, session.sessionId)
          HttpMethod.Post -> {
            val body = call.receive<Map<String, Any?>>()
            memberService.callBackendApiPost(fullPath, session.sessionId, username, body)
          }
          HttpMethod.Patch -> {
            val body = call.receive<Map<String, Any?>>()
            memberService.callBackendApiPatch(fullPath, session.sessionId, username, body)
          }
          HttpMethod.Delete ->
              memberService.callBackendApiDelete(fullPath, session.sessionId, username)
          else ->
              throw IllegalArgumentException("Unsupported HTTP method: ${call.request.httpMethod}")
        }
    call.respond(result)
  }
}
