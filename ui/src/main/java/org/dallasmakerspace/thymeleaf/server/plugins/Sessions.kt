package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.util.*

fun Application.configureSessions() {
  install(Sessions) { cookie<UserSession>("user_session", SessionStorageMemory()) }
}

data class UserSession(
    var accessToken: String? = null,
    var idHint: String? = null,
    var adPrincipalUser: Map<String, Any> = mapOf(),
    var isDiscourseLinkSuccess: Boolean = false,
) : Principal
