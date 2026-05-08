package org.dallasmakerspace.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicBoolean
import org.dallasmakerspace.activedirectory.ADException
import org.dallasmakerspace.members.MemberService

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

fun Application.configureHealth(memberService: MemberService) {
  monitor.subscribe(ApplicationStopping) { HealthState.markNotReady() }

  routing {
    get("/health/live") { call.respond(HttpStatusCode.OK, "ok") }
    get("/health/ready") {
      if (!HealthState.isReady()) {
        call.respond(HttpStatusCode.ServiceUnavailable, "not ready")
        return@get
      }
      try {
        memberService.getMemberByUsername("test")
      } catch (_: ADException) {
        // "test" not found in AD — AD is still reachable, probe passes
      } catch (e: Exception) {
        call.respond(HttpStatusCode.ServiceUnavailable, "probe failed: ${e.message}")
        return@get
      }
      call.respond(HttpStatusCode.OK, "ok")
    }
  }
}
