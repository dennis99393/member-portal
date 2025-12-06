package org.dallasmakerspace.plugins

import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.auth.ApiKeyAuthProvider
import org.dallasmakerspace.core.logging.ElasticsearchClientManager
import org.dallasmakerspace.di.DaggerAppComponent
import org.slf4j.MDC

fun Application.configureElasticsearch() {
  // Elasticsearch logging is handled by logback ELASTIC appender
  // This function is kept for future use if needed
}
