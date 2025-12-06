package org.dallasmakerspace.server.plugins

import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dallasmakerspace.server.common.logging.ElasticsearchClientManager
import org.dallasmakerspace.server.di.DaggerAppComponent

fun Application.configureElasticsearch() {
  // Elasticsearch logging is handled by logback ELASTIC appender
  // This function is kept for future use if needed
}
