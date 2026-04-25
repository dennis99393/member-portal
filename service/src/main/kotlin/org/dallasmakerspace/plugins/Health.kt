package org.dallasmakerspace.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicBoolean

object HealthState {
  private val ready = AtomicBoolean(false)

  fun markReady() {
    ready.set(true)
  }

  fun markNotReady() {
    ready.set(false)
  }

  fun isReady(): Boolean = ready.get()
}

fun Application.configureHealth() {
  monitor.subscribe(ApplicationStopping) { HealthState.markNotReady() }

  routing {
    get("/health/live") { call.respond(HttpStatusCode.OK, "ok") }
    get("/health/ready") {
      if (HealthState.isReady()) {
        call.respond(HttpStatusCode.OK, "ok")
      } else {
        call.respond(HttpStatusCode.ServiceUnavailable, "not ready")
      }
    }
  }
}
