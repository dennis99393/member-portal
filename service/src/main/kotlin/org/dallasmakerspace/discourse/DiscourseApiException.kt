package org.dallasmakerspace.discourse

import java.io.IOException

class DiscourseApiException : IOException {
  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable?) : super(message, cause)
}
