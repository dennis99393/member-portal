package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory

class IndexHandler
@Inject
constructor(loggerFactory: LoggerFactory, userInfoProvider: UserInfoProvider) :
    AuthRouteHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {

    log.info("Handle request: ${call.request}")
    if (userInfo.isEmpty().not()) {
      val jsonMap: MutableMap<String, Any> = userInfo.toMutableMap()
      jsonMap["profile_url"] = "./profile/@${jsonMap["preferred_username"]}"
      call.respond(ThymeleafContent("index", jsonMap))
    }
  }
}
