package org.dallasmakerspace.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import org.dallasmakerspace.server.common.logging.ElasticsearchClientManager
import org.dallasmakerspace.server.common.logging.logToElasticsearch

fun Application.configureElasticsearch() {
  intercept(ApplicationCallPipeline.Plugins) {
    logToElasticsearch(call, ElasticsearchClientManager.client)
  }
}
