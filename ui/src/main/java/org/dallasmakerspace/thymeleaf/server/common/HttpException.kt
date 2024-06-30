package org.dallasmakerspace.thymeleaf.server.common

class HttpException(message: String, cause: Throwable?) : Exception(message, cause) {
  constructor(message: String) : this(message, null)
}
