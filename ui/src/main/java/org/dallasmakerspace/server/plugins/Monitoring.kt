package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import org.slf4j.MDC
import org.slf4j.event.Level

fun Application.configureMonitoring() {
  install(CallLogging) {
    level = Level.INFO
    // filter { call -> call.request.path().startsWith("/static").not() } // Don't log static

    mdc("sessionid") { call ->
      try {
        val session = call.sessions.get<UserSession>()
        val sessionId = session?.sessionId
        call.application.log.info("MDC sessionid - Session exists: ${session != null}, SessionId: $sessionId")
        sessionId
      } catch (e: Exception) {
        call.application.log.error("MDC sessionid - Exception getting session", e)
        null
      }
    }

    mdc("userid") { call ->
      try {
        val session = call.sessions.get<UserSession>()
        val userId = session?.userId
        call.application.log.info("MDC userid - Session exists: ${session != null}, UserId: $userId")
        userId
      } catch (e: Exception) {
        call.application.log.error("MDC userid - Exception getting session", e)
        null
      }
    }

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
