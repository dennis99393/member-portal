package org.dallasmakerspace.core.logging

import io.ktor.util.logging.*
import org.dallasmakerspace.core.appLogger
import org.slf4j.Marker

/** A simple logger that logs to memory as well as an slf4j logger */
@Suppress("TooManyFunctions")
class AppInMemoryLogger(clazz: Class<*>) : Logger {
  private val logger = appLogger(clazz.simpleName)
  private val logBuffer = StringBuilder()

  fun getLog(): String {
    return logBuffer.toString()
  }

  fun clear() {
    logBuffer.setLength(0)
  }

  override fun getName(): String = logger.name

  override fun isTraceEnabled(): Boolean = logger.isTraceEnabled

  override fun debug(message: String?) {
    logger.debug(message)
  }

  override fun info(message: String?) {
    logger.info(message)
    logBuffer.append("$message\n")
  }

  override fun error(message: String?, throwable: Throwable?) {
    logger.error(message, throwable)
    logBuffer.append("$message; ${throwable?.message}\n")
  }

  override fun isTraceEnabled(p0: Marker?): Boolean = logger.isTraceEnabled(p0)

  override fun trace(p0: String?) = throw NotImplementedError()

  override fun trace(p0: String?, p1: Any?) = throw NotImplementedError()

  override fun trace(p0: String?, p1: Any?, p2: Any?) = throw NotImplementedError()

  override fun trace(p0: String?, vararg p1: Any?) = throw NotImplementedError()

  override fun trace(p0: String?, p1: Throwable?) = throw NotImplementedError()

  override fun trace(p0: Marker?, p1: String?) = throw NotImplementedError()

  override fun trace(p0: Marker?, p1: String?, p2: Any?) = throw NotImplementedError()

  override fun trace(p0: Marker?, p1: String?, p2: Any?, p3: Any?) = throw NotImplementedError()

  override fun trace(p0: Marker?, p1: String?, vararg p2: Any?) = throw NotImplementedError()

  override fun trace(p0: Marker?, p1: String?, p2: Throwable?) = throw NotImplementedError()

  override fun isDebugEnabled(): Boolean = throw NotImplementedError()

  override fun isDebugEnabled(p0: Marker?): Boolean = throw NotImplementedError()

  override fun debug(p0: String?, p1: Any?) = throw NotImplementedError()

  override fun debug(p0: String?, p1: Any?, p2: Any?) = throw NotImplementedError()

  override fun debug(p0: String?, vararg p1: Any?) = throw NotImplementedError()

  override fun debug(p0: String?, p1: Throwable?) = throw NotImplementedError()

  override fun debug(p0: Marker?, p1: String?) = throw NotImplementedError()

  override fun debug(p0: Marker?, p1: String?, p2: Any?) = throw NotImplementedError()

  override fun debug(p0: Marker?, p1: String?, p2: Any?, p3: Any?) = throw NotImplementedError()

  override fun debug(p0: Marker?, p1: String?, vararg p2: Any?) = throw NotImplementedError()

  override fun debug(p0: Marker?, p1: String?, p2: Throwable?) = throw NotImplementedError()

  override fun isInfoEnabled(): Boolean = throw NotImplementedError()

  override fun isInfoEnabled(p0: Marker?): Boolean = throw NotImplementedError()

  override fun info(p0: String?, p1: Any?) = throw NotImplementedError()

  override fun info(p0: String?, p1: Any?, p2: Any?) = throw NotImplementedError()

  override fun info(p0: String?, vararg p1: Any?) = throw NotImplementedError()

  override fun info(p0: String?, p1: Throwable?) = throw NotImplementedError()

  override fun info(p0: Marker?, p1: String?) = throw NotImplementedError()

  override fun info(p0: Marker?, p1: String?, p2: Any?) = throw NotImplementedError()

  override fun info(p0: Marker?, p1: String?, p2: Any?, p3: Any?) = throw NotImplementedError()

  override fun info(p0: Marker?, p1: String?, vararg p2: Any?) = throw NotImplementedError()

  override fun info(p0: Marker?, p1: String?, p2: Throwable?) = throw NotImplementedError()

  override fun isWarnEnabled(): Boolean = throw NotImplementedError()

  override fun isWarnEnabled(p0: Marker?): Boolean = throw NotImplementedError()

  override fun warn(p0: String?) = throw NotImplementedError()

  override fun warn(p0: String?, p1: Any?) = throw NotImplementedError()

  override fun warn(p0: String?, vararg p1: Any?) = throw NotImplementedError()

  override fun warn(p0: String?, p1: Any?, p2: Any?) = throw NotImplementedError()

  override fun warn(p0: String?, p1: Throwable?) = throw NotImplementedError()

  override fun warn(p0: Marker?, p1: String?) = throw NotImplementedError()

  override fun warn(p0: Marker?, p1: String?, p2: Any?) = throw NotImplementedError()

  override fun warn(p0: Marker?, p1: String?, p2: Any?, p3: Any?) = throw NotImplementedError()

  override fun warn(p0: Marker?, p1: String?, vararg p2: Any?) = throw NotImplementedError()

  override fun warn(p0: Marker?, p1: String?, p2: Throwable?) = throw NotImplementedError()

  override fun isErrorEnabled(): Boolean = throw NotImplementedError()

  override fun isErrorEnabled(p0: Marker?): Boolean = throw NotImplementedError()

  override fun error(p0: String?) = throw NotImplementedError()

  override fun error(p0: String?, p1: Any?) = throw NotImplementedError()

  override fun error(p0: String?, p1: Any?, p2: Any?) = throw NotImplementedError()

  override fun error(p0: String?, vararg p1: Any?) = throw NotImplementedError()

  override fun error(p0: Marker?, p1: String?) = throw NotImplementedError()

  override fun error(p0: Marker?, p1: String?, p2: Any?) = throw NotImplementedError()

  override fun error(p0: Marker?, p1: String?, p2: Any?, p3: Any?) = throw NotImplementedError()

  override fun error(p0: Marker?, p1: String?, vararg p2: Any?) = throw NotImplementedError()

  override fun error(p0: Marker?, p1: String?, p2: Throwable?) = throw NotImplementedError()
}
