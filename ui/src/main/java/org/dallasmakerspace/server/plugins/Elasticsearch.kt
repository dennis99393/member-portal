package org.dallasmakerspace.server.plugins

import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.server.common.logging.ElasticsearchClientManager
import org.dallasmakerspace.server.di.DaggerAppComponent
import org.slf4j.MDC

fun Application.configureElasticsearch() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  val isDevelopment = appConfig.requireBooleanProperty("ktor.development")
  if (!isDevelopment) {
    intercept(ApplicationCallPipeline.Monitoring) {
      logToElasticsearch(this.call, ElasticsearchClientManager.client)
    }
  }
}

suspend fun logToElasticsearch(call: PipelineCall, client: ElasticsearchClient) {
  val request = call.request
  val response = call.response
  // Do not log redirect responses and static files
  if (
      response.status() == HttpStatusCode.TemporaryRedirect ||
          request.uri.startsWith("/static") ||
          request.uri.startsWith("/favicon.ico")
  ) {
    return
  }

  val logEntry =
      mapOf(
          "@timestamp" to Instant.now().toString(),
          "method" to request.httpMethod.value,
          "app" to "member-profile-ui",
          "host" to request.host(),
          "ip" to request.origin.remoteAddress,
          "sessionid" to (MDC.get("sessionid") ?: "NO_SESSION"),
          "userid" to (MDC.get("userid") ?: "NO_USER"),
          "uri" to request.uri,
          "status" to response.status()?.value,
          "userAgent" to request.userAgent(),
      )

  withContext(Dispatchers.IO) {
    try {
      client.index { i -> i.index("logs").document(logEntry) }
    } catch (e: IOException) {
      ElasticsearchClientManager.log.error("Failed to log to Elasticsearch", e)
    }
  }
}
