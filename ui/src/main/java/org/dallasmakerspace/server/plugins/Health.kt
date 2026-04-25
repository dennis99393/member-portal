package org.dallasmakerspace.server.plugins

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
  environment.monitor.subscribe(ApplicationStopping) { HealthState.markNotReady() }

  routing {
    get("/healthz") { call.respond(HttpStatusCode.OK, "ok") }
    get("/readyz") {
      if (HealthState.isReady()) {
        call.respond(HttpStatusCode.OK, "ok")
      } else {
        call.respond(HttpStatusCode.ServiceUnavailable, "not ready")
      }
    }
  }
}
