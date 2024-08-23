package org.dallasmakerspace.thymeleaf.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File
import java.net.URL
import java.security.MessageDigest
import org.dallasmakerspace.thymeleaf.server.routes.RouteFactory

fun Application.configureRouting() {

  routing {
    authenticate("DMS") {
      get(RouteFactory.Paths.LOGIN.path) { RouteFactory.getHandler(call)?.handle(call) }
      get(RouteFactory.Paths.OIDC_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.OIDC_CALLBACK.path)?.handle(call)
      }
    }

    get(RouteFactory.Paths.INDEX.path) { RouteFactory.getHandler(call)?.handle(call) }
    get(RouteFactory.Paths.PROFILE.path) { RouteFactory.getHandler(call)?.handle(call) }
    get(RouteFactory.Paths.DISCOURSE_LINK.path) { RouteFactory.getHandler(call)?.handle(call) }
    get(RouteFactory.Paths.DISCOURSE_UNLINK.path) { RouteFactory.getHandler(call)?.handle(call) }
    get(RouteFactory.Paths.DISCOURSE_CALLBACK.path) {
      RouteFactory.getHandler(RouteFactory.Paths.DISCOURSE_CALLBACK.path)?.handle(call)
    }
    get(RouteFactory.Paths.GROUPS.path) { RouteFactory.getHandler(call)?.handle(call) }

    get(RouteFactory.Paths.PING.path) { RouteFactory.getHandler(call)?.handle(call) }

    staticResources(RouteFactory.Paths.STATIC.path, "static") {
      cacheControl {
        listOf(CacheControl.MaxAge(maxAgeSeconds = 300)) // 5 min
      }
      enableAutoHeadResponse()
      modify { url, call ->
        val file = url.toFile()
        if (file.exists()) {
          val eTag = calculateETag(file)
          call.response.headers.append(HttpHeaders.ETag, eTag)
        }
      }
    }
  }
}

private fun URL.toFile(): File {
  return File(this.file)
}

private fun calculateETag(file: File): String {
  val lastModified = file.lastModified()
  val size = file.length()
  val hash =
      MessageDigest.getInstance("MD5").digest("$lastModified$size".toByteArray()).fold("") {
          str,
          acc ->
        str + "%02x".format(acc)
      }
  return "\"$hash\""
}
