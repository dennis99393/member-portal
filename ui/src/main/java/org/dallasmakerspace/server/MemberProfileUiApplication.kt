package org.dallasmakerspace.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.dallasmakerspace.server.common.PostHogConfig
import org.dallasmakerspace.server.common.TrustManager
import org.dallasmakerspace.server.di.DaggerAppComponent
import org.dallasmakerspace.server.plugins.configureElasticsearch
import org.dallasmakerspace.server.plugins.configureHttp
import org.dallasmakerspace.server.plugins.configureMonitoring
import org.dallasmakerspace.server.plugins.configureRouting
import org.dallasmakerspace.server.plugins.configureSessionEnrichment
import org.dallasmakerspace.server.plugins.configureSessions
import org.dallasmakerspace.server.plugins.configureStatusPages
import org.dallasmakerspace.server.plugins.configureTemplating

fun main() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  PostHogConfig.initialize(appConfig)
  val portStr = appConfig.requireStringProperty("ktor.deployment.port")

  // Disable SSL certificate verification until we can embed our root CA cert sig
  // TODO: remove this
  TrustManager.disableSSLCertificateChecking()

  embeddedServer(Netty, port = portStr.toInt(), host = "0.0.0.0") {
        configureTemplating()
        configureHttp()
        configureSessions()
        configureSessionEnrichment() // Must run after Sessions/Auth but before Monitoring
        configureMonitoring()
        configureRouting()
        configureStatusPages()
        configureElasticsearch()
      }
      .start(wait = true)
}
