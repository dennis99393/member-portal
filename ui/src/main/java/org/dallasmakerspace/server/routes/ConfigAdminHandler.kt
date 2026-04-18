package org.dallasmakerspace.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.auth.Permission
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberServiceClient

class ConfigAdminHandler
@Inject
constructor(
    loggerFactory: LoggerFactory,
    userInfoProvider: UserInfoProvider,
    private val memberServiceClient: MemberServiceClient,
) : AuthenticatedHandler(loggerFactory, userInfoProvider) {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handleAuthenticated(call: ApplicationCall) {
    if (!call.require(Permission.MANAGE_CONFIG)) return

    val username = userInfo["preferred_username"] as String? ?: ""
    val configList = memberServiceClient.getConfigList(session.sessionId)

    call.respond(
        ThymeleafContent(
            "admin-config",
            mapOf(
                "username" to username,
                "authz" to authz,
                "config_list" to configList,
            ),
        ))
  }
}
