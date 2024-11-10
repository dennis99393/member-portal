package org.dallasmakerspace.server.common.logging

import io.ktor.util.logging.*
import javax.inject.Inject

class LoggerFactory @Inject constructor() {
  fun create(clazz: Class<*>): Logger = appLogger(clazz.simpleName)
}
