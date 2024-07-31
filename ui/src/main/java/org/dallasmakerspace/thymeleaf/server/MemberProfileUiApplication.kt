package org.dallasmakerspace.thymeleaf.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.dallasmakerspace.thymeleaf.server.common.TrustManager
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent
import org.dallasmakerspace.thymeleaf.server.plugins.configureElasticsearch
import org.dallasmakerspace.thymeleaf.server.plugins.configureHttp
import org.dallasmakerspace.thymeleaf.server.plugins.configureMonitoring
import org.dallasmakerspace.thymeleaf.server.plugins.configureRouting
import org.dallasmakerspace.thymeleaf.server.plugins.configureSessions
import org.dallasmakerspace.thymeleaf.server.plugins.configureStatusPages
import org.dallasmakerspace.thymeleaf.server.plugins.configureTemplating

fun main() {
  val appConfig = DaggerAppComponent.create().getAppConfig()
  val portStr = appConfig.requireStringProperty("ktor.deployment.port")

  // Disable SSL certificate verification until we can embed our root CA cert sig
  // TODO: remove this
  TrustManager.disableSSLCertificateChecking()

  embeddedServer(Netty, port = portStr.toInt(), host = "0.0.0.0") {
        configureTemplating()
        configureHttp()
        configureRouting()
        configureStatusPages()
        configureMonitoring()
        configureSessions()
        configureElasticsearch()
      }
      .start(wait = true)
}
