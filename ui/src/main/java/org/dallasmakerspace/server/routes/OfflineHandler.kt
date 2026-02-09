package org.dallasmakerspace.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import javax.inject.Inject
import org.dallasmakerspace.server.common.logging.LoggerFactory

class OfflineHandler @Inject constructor(loggerFactory: LoggerFactory) : IRouteHandler {
  private val log = loggerFactory.create(javaClass)

  override suspend fun handle(call: ApplicationCall) {
    log.debug("OfflineHandler serving offline page")

    call.respond(ThymeleafContent("offline", mapOf("title" to "Offline - DMS Member Portal")))
  }
}
