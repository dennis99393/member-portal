package org.dallasmakerspace.thymeleaf.server.discourse

import io.ktor.server.application.*
import io.ktor.server.response.*
import javax.inject.Inject
import kotlin.random.Random
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.plugins.AuthException
import org.dallasmakerspace.thymeleaf.server.routes.AuthRouteHandler

private const val NONCE_RANGE_START = 1000000
private const val NONCE_RANGE_END = 9999999

class LinkDiscourseHandler
@Inject
constructor(
  loggerFactory: LoggerFactory,
    private val userInfoProvider: UserInfoProvider,
    private val discourseNonceCache: DiscourseNonceCache,
    private val discourseLinkProvider: DiscourseSSOProvider
) : AuthRouteHandler(loggerFactory, userInfoProvider) {
  override suspend fun handle(call: ApplicationCall) {
    super.handle(call)
    val username =
        userInfo["preferred_username"] as? String
            ?: throw AuthException("No username found in user info")
    val randomNonce = Random.nextInt(NONCE_RANGE_START, NONCE_RANGE_END).toString()
    discourseNonceCache.setNonce(username, randomNonce)
    val discourseSSOConnectUrl = discourseLinkProvider.getDiscourseSSOConnectUrl(randomNonce)
    call.respondRedirect(discourseSSOConnectUrl, permanent = false)
  }
}
