package org.dallasmakerspace.server.plugins

import io.ktor.server.application.*

fun Application.configureElasticsearch() {
  intercept(ApplicationCallPipeline.Monitoring) {
    // logToElasticsearch(ElasticsearchClientManager.client)
  }
}
