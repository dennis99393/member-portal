package org.dallasmakerspace.thymeleaf.server.common.logging

import com.agido.logback.elasticsearch.config.Authentication
import java.net.HttpURLConnection
import org.dallasmakerspace.thymeleaf.server.di.DaggerAppComponent

class ElasticsearchAuth : Authentication {
  private val appConfig = DaggerAppComponent.create().getAppConfig()
  private val apiKey = appConfig.requireStringProperty("app.logging.elastic-api-key")

  override fun addAuth(urlConnection: HttpURLConnection, body: String) {
    urlConnection.setRequestProperty("Authorization", "ApiKey $apiKey")
  }
}
