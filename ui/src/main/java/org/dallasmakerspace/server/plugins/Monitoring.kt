package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import org.slf4j.MDC
import org.slf4j.event.Level

fun Application.configureMonitoring() {
  // Set MDC values for the entire request lifecycle
  intercept(ApplicationCallPipeline.Monitoring) {
    val session = try {
      call.sessions.get<UserSession>()
    } catch (e: Exception) {
      null
    }
    val sessionId = session?.sessionId
    val userId = session?.userId

    val useridCloseable = MDC.putCloseable("userid", userId)
    val sessionidCloseable = MDC.putCloseable("sessionid", sessionId)

    try {
      proceed()
    } finally {
      useridCloseable?.close()
      sessionidCloseable?.close()
    }
  }

  install(CallLogging) {
    level = Level.INFO
    // filter { call -> call.request.path().startsWith("/static").not() } // Don't log static

    format { call ->
      val status = call.response.status()
      val httpMethod = call.request.httpMethod.value
      val ipAddress = call.request.origin.remoteHost
      val path = call.request.path()
      val timeTaken = call.processingTimeMillis()
      val userAgent = call.request.headers["User-Agent"]
      "IP: $ipAddress, Status: $status, HTTP: $httpMethod, URL: $path, Time: ${timeTaken}ms, User agent: $userAgent"
    }
  }
}
