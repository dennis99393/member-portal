package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.client.engine.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.logging.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import io.ktor.util.date.*
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent
import org.slf4j.MDC
import org.slf4j.event.Level

fun Application.configureMonitoring() {
  val loggerFactory = DaggerAppComponent.create().getLoggerFactory()
  install(CallLogging) {
    level = Level.INFO
    logger = loggerFactory.create(Application::class.java)
    filter { call -> call.request.path().startsWith("/static").not() } // Don't log static

    format { call ->
      val status = call.response.status()
      val httpMethod = call.request.httpMethod.value
      val ipAddress = call.request.origin.remoteAddress
      val path = call.request.path()
      val session = call.sessions.get<UserSession>()
      val sessionId = session?.sessionId
      val userId = session?.userId
      MDC.putCloseable("userid", userId)
      MDC.putCloseable("sessionid", sessionId)
      val timeTaken = call.processingTimeMillis { getTimeMillis() }
      val userAgent = call.request.headers["User-Agent"]
      "IP: $ipAddress, Status: $status, HTTP: $httpMethod, URL: $path, Time: ${timeTaken}ms, User agent: $userAgent"
    }
  }
}
