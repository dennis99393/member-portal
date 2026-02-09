package org.dallasmakerspace.server.common

object PostHogConfig {
  private var apiKey: String = ""
  private var host: String = ""

  fun initialize(appConfig: AppConfig) {
    apiKey = appConfig.getStringProperty("app.posthog.api-key", "")
    host = appConfig.getStringProperty("app.posthog.host", "https://us.i.posthog.com")
  }

  @JvmStatic fun isEnabled(): Boolean = apiKey.isNotBlank()

  @JvmStatic fun getApiKey(): String = apiKey

  @JvmStatic fun getHost(): String = host
}
