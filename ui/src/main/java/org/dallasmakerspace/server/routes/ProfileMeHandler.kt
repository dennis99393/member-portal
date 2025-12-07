package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import kotlin.collections.set
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory

class ProfileMeHandler
@Inject
constructor(loggerFactory: LoggerFactory, userInfoProvider: UserInfoProvider) :
    AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {

    val username = userInfo["preferred_username"] as String

    log.debug("Username: {}", username)
    call.respondRedirect("/profile/@$username", permanent = false)
  }
}
