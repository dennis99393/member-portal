package org.dallasmakerspace.plugins

import io.ktor.server.application.*
import org.dallasmakerspace.core.logging.ElasticsearchClientManager
import org.dallasmakerspace.core.logging.logToElasticsearch

fun Application.configureElasticsearch() {
  intercept(ApplicationCallPipeline.Monitoring) {
    logToElasticsearch(ElasticsearchClientManager.client)
  }
}
