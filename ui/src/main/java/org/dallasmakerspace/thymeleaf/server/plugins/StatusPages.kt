package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*

fun Application.configureStatusPages() {
  install(StatusPages) {
    status(HttpStatusCode.NotFound) { call, status ->
      call.respond(status = status, ThymeleafContent("error404", mapOf("message" to "Sorry! Page was not found.")))
    }
  }
}
