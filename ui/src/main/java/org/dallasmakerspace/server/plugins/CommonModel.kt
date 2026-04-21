package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.thymeleaf.*
import org.dallasmakerspace.server.remoteaccess.RemoteAccessFeatureFlag
import org.dallasmakerspace.server.routes.AuthenticatedHandler

private val CommonModelPlugin =
    createApplicationPlugin("CommonModel") {
      onCallRespond { call ->
        transformBody { body ->
          if (body is ThymeleafContent) {
            val isInfraUser =
                call.attributes.getOrNull(AuthenticatedHandler.IS_INFRA_USER_KEY) ?: false
            val remoteAccessEnabled = RemoteAccessFeatureFlag.isEnabled(isInfraUser)
            ThymeleafContent(
                body.template,
                body.model +
                    mapOf(
                        "isInfraUser" to isInfraUser, "remoteAccessEnabled" to remoteAccessEnabled),
                body.etag,
                body.contentType,
                body.locale,
            )
          } else body
        }
      }
    }

fun Application.configureCommonModel() {
  install(CommonModelPlugin)
}
