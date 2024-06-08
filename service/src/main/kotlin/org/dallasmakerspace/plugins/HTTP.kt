package org.dallasmakerspace.plugins

import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleMemoryCache.memoryCache
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureHTTP() {
  install(SimpleCache) { memoryCache { invalidateAt = 10.seconds } }
  @Suppress("MagicNumber")
  install(Compression) {
    gzip { priority = 1.0 }
    deflate {
      priority = 10.0
      minimumSize(1024) // condition
    }
  }
}
