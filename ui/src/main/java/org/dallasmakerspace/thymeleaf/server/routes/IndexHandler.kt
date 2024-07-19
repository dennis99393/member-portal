package org.dallasmakerspace.thymeleaf.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.LoggerFactory

class IndexHandler
@Inject
constructor(loggerFactory: LoggerFactory, userInfoProvider: UserInfoProvider) :
    AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    log.info("Handle request: ${call.request}")
    val jsonMap: MutableMap<String, Any> = userInfo.toMutableMap()
    jsonMap["profile_url"] = "./profile/@${jsonMap["preferred_username"]}"
    call.respond(ThymeleafContent("index", jsonMap))
  }
}
