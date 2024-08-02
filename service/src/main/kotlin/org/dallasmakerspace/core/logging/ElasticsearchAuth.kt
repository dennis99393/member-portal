package org.dallasmakerspace.core.logging

import com.agido.logback.elasticsearch.config.Authentication
import java.net.HttpURLConnection
import org.dallasmakerspace.di.DaggerAppComponent

class ElasticsearchAuth : Authentication {
  private val appConfig = DaggerAppComponent.create().getAppConfig()
  private val apiKey = appConfig.requireStringProperty("app.logging.elastic-api-key")

  override fun addAuth(urlConnection: HttpURLConnection, body: String) {
    urlConnection.setRequestProperty("Authorization", "ApiKey $apiKey")
  }
}
