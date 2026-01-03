package org.dallasmakerspace.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Principal
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

fun Application.configureSessions() {
  install(Sessions) { cookie<UserSession>("user_session", SessionStorageMemory()) }
}

@Serializable
data class UserSession(
    var accessToken: String? = null,
    var idHint: String? = null,
    var adPrincipalUser: JsonObject = JsonObject(mapOf()),
    var isDiscourseLinkSuccess: Boolean = false,
    var isDiscordLinkSuccess: Boolean = false,
    var isLinkedInLinkSuccess: Boolean = false,
    var isVoterRegistrationSuccess: Boolean = false,
    var sessionId: String? = null,
    var userId: String? = null,
) : Principal
