package org.dallasmakerspace.server.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.URL
import java.security.MessageDigest
import org.dallasmakerspace.server.routes.RouteFactory

fun Application.configureRouting() {

  routing {
    authenticate("DMS") {
      get(RouteFactory.Paths.LOGIN.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.OIDC_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.OIDC_CALLBACK.path)?.handleBase(call)
      }
    }

    // Protected routes using session authentication
    authenticate("auth_session") {
      get(RouteFactory.Paths.INDEX.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.PROFILE.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.PROFILEME.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.GROUPS.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.SEARCH_PRELOAD.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.REPORT.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.SHORT_LINKS.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.SHORT_LINKS_ADMIN.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.SHORT_LINK_DETAILS.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.BACKEND_API.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      post(RouteFactory.Paths.BACKEND_API.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      patch(RouteFactory.Paths.BACKEND_API.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      delete(RouteFactory.Paths.BACKEND_API.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.DISCOURSE_LINK.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.DISCOURSE_UNLINK.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.DISCOURSE_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.DISCOURSE_CALLBACK.path)?.handleBase(call)
      }
      get(RouteFactory.Paths.DISCORD_LINK.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.DISCORD_UNLINK.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.DISCORD_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.DISCORD_CALLBACK.path)?.handleBase(call)
      }
      get(RouteFactory.Paths.LINKEDIN_LINK.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.LINKEDIN_UNLINK.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.LINKEDIN_CALLBACK.path) {
        RouteFactory.getHandler(RouteFactory.Paths.LINKEDIN_CALLBACK.path)?.handleBase(call)
      }
      get(RouteFactory.Paths.REGISTER_VOTING.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.UNREGISTER_VOTING.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.SHORT_LINK_REDIRECT.path) {
        RouteFactory.getHandler(call)?.handleBase(call)
      }
      get(RouteFactory.Paths.COMMITTEES.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      post(RouteFactory.Paths.ACTION_TRACK.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get(RouteFactory.Paths.ASK_AI.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      post(RouteFactory.Paths.ASK_AI.path) { RouteFactory.getHandler(call)?.handleBase(call) }
      get("/ask-ai/q/{slug}") {
        RouteFactory.getHandler(RouteFactory.Paths.ASK_AI.path)?.handleBase(call)
      }
      post("/ask-ai/feedback") {
        RouteFactory.getHandler(RouteFactory.Paths.ASK_AI.path)?.handleBase(call)
      }
    }

    get(RouteFactory.Paths.PING.path) { RouteFactory.getHandler(call)?.handleBase(call) }

    // Block all bots from crawling the site
    get("robots.txt") {
      call.respond(
          TextContent(
              """
              User-agent: *
              Disallow: /
              """
                  .trimIndent(),
              ContentType.Text.Plain,
              HttpStatusCode.OK,
          ),
      )
    }

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
