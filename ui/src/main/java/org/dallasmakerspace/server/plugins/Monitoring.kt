package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import org.slf4j.MDC
import org.slf4j.event.Level

fun Application.configureMonitoring() {
  // Set MDC early so it's available for both CallLogging and Elasticsearch interceptor
  intercept(ApplicationCallPipeline.Setup) {
    val session = try {
      call.sessions.get<UserSession>()
    } catch (e: Exception) {
      null
    }
    val sessionId = session?.sessionId
    val userId = session?.userId

    MDC.put("userid", userId)
    MDC.put("sessionid", sessionId)

    try {
      proceed()
    } finally {
      MDC.remove("userid")
      MDC.remove("sessionid")
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
