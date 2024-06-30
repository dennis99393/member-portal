package org.dallasmakerspace.core

import io.ktor.server.config.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject constructor() {
  private val config: ApplicationConfig = ApplicationConfig(null)

  private fun requireProperty(name: String): ApplicationConfigValue {
    return requireNotNull(config.propertyOrNull(name)) {
      "$TAG/requireProperty $name is not configured"
    }
  }

  fun requireBooleanProperty(name: String): Boolean {
    return requireProperty(name).getString().toBoolean()
  }

  fun requireStringProperty(name: String): String {
    return requireProperty(name).getString()
  }

  fun requireIntProperty(name: String): Int {
    return requireProperty(name).getString().toInt()
  }

  companion object {
    private const val TAG = "AppConfig"
  }
}
