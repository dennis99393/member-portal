package org.dallasmakerspace.thymeleaf.server.common

import org.slf4j.LoggerFactory

object Log {
  private val logger = LoggerFactory.getLogger("logger")

  fun i(message: String) {
    logger.info(message)
  }

  fun e(message: String, e: Throwable?) {
    logger.error(message, e)
  }
}
