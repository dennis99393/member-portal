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
        call.sessions.get<UserSession>()?.sessionId
      } catch (e: Exception) {
        null
      }
    }

    mdc("userid") { call ->
      try {
        call.sessions.get<UserSession>()?.userId
      } catch (e: Exception) {
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
