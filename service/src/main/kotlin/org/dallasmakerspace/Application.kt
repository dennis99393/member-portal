package org.dallasmakerspace

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.dallasmakerspace.core.logging.TrustManager
import org.dallasmakerspace.di.DaggerAppComponent
import org.dallasmakerspace.plugins.configureDatabase
import org.dallasmakerspace.plugins.configureElasticsearch
import org.dallasmakerspace.plugins.configureHTTP
import org.dallasmakerspace.plugins.configureMonitoring
import org.dallasmakerspace.plugins.configureRouting
import org.dallasmakerspace.plugins.configureSerialization

fun main() {

  val appConfig = DaggerAppComponent.create().getAppConfig()
  val portStr = appConfig.requireStringProperty("ktor.deployment.port")

  // Disable SSL certificate verification until we can embed our root CA cert sig
  // TODO: remove this
  TrustManager.disableSSLCertificateChecking()

  embeddedServer(Netty, port = portStr.toInt(), host = "0.0.0.0") {
        configureSerialization()
        configureMonitoring()
        configureHTTP()
        configureRouting()
        configureDatabase()
        configureElasticsearch()
      }
      .start(wait = true)
}
