package org.dallasmakerspace.core

import javax.inject.Inject

class Log @Inject constructor() {
  // TODO(mandarl): Use ktor logger instead of println
  fun i(message: String) {
    println("INFO: $message")
  }

  fun e(message: String) {
    println("ERROR: $message")
  }

  fun w(message: String, e: Throwable) {
    println("WARN: $message; ${e.stackTraceToString()}")
  }
}
