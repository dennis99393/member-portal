package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*
import org.dallasmakerspace.server.common.logging.ElasticsearchClientManager
import org.dallasmakerspace.server.common.logging.logToElasticsearch

fun Application.configureElasticsearch() {
  intercept(ApplicationCallPipeline.Monitoring) {
    logToElasticsearch(ElasticsearchClientManager.client)
  }
}
