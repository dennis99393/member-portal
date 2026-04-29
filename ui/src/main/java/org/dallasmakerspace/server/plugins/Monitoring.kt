package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level

fun Application.configureMonitoring() {
  val log = LoggerFactory.getLogger("Monitoring")

  // Set MDC early so it's available for both CallLogging and Elasticsearch interceptor
  intercept(ApplicationCallPipeline.Call) {
    val session =
        try {
          call.sessions.get<UserSession>()
        } catch (e: Exception) {
          null
        }
    val sessionId = session?.sessionId
    val userId = session?.userId
    log.debug("Setting MDC values: sessionId=$sessionId, userId=$userId")

    MDC.put("userid", userId ?: "NO_USER")
    MDC.put("sessionid", sessionId ?: "NO_SESSION")

    try {
      proceed()
    } finally {
      MDC.remove("userid")
      MDC.remove("sessionid")
    }
  }

  install(CallLogging) {
    level = Level.INFO
    filter { call ->
      val path = call.request.path()
      !path.startsWith("/static") &&
          path != "/readyz" &&
          path != "/healthz" &&
          path != "/sw.js" &&
          path != "/manifest.json" &&
          path != "/favicon.ico" &&
          path != "/apple-touch-icon.png" &&
          path != "/apple-touch-icon-precomposed.png"
    }

    format { call ->
      val status = call.response.status()
      val httpMethod = call.request.httpMethod.value
      val ipAddress = call.request.origin.remoteHost
      val path = call.request.path()
      val timeTaken = call.processingTimeMillis()
      val userAgent = call.request.headers["User-Agent"]
      val session = call.sessions.get<UserSession>()
      val sessionId = session?.sessionId ?: "NO_SESSION"
      val userId = session?.userId ?: "NO_USER"
      MDC.putCloseable("userid", userId)
      MDC.putCloseable("sessionid", sessionId)
      "SessionId: $sessionId, UserId: $userId, IP: $ipAddress, Status: $status, HTTP: $httpMethod, URL: $path, Time: ${timeTaken}ms, User agent: $userAgent"
    }
  }
}
