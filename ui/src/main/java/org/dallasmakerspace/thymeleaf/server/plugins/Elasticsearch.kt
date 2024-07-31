package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.server.application.*
import org.dallasmakerspace.thymeleaf.server.common.logging.ElasticsearchClientManager
import org.dallasmakerspace.thymeleaf.server.common.logging.logToElasticsearch

fun Application.configureElasticsearch() {
  intercept(ApplicationCallPipeline.Monitoring) {
    logToElasticsearch(ElasticsearchClientManager.client)
  }
}
