package org.dallasmakerspace.server.common

class HttpException(message: String, val httpStatus: Int = 0, cause: Throwable? = null) :
    Exception(message, cause) {
  constructor(message: String) : this(message, 0, null)
}
