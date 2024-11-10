package org.dallasmakerspace.server.routes

import dagger.Component
import io.ktor.server.application.*
import io.ktor.server.request.*
import javax.inject.Singleton
import org.dallasmakerspace.server.di.DiscourseModule
import org.dallasmakerspace.server.di.RoutesModule

object RouteFactory {
  val redirects = mutableMapOf<String, String>()

  fun getHandler(call: ApplicationCall): IRouteHandler? {
    val path = sanitizePathInternal(call.request.uri)
    return getHandler(path)
  }

  private fun sanitizePathInternal(path: String): String {
    // Replace "@.*" with "@{preferred_username}"
    return path
        .replace("@.*".toRegex(), "@{preferred_username}")
        .replace("/groups/.*".toRegex(), "/groups/{group_slug}")
  }

  fun getHandler(path: String): IRouteHandler? {
    val routesMap = DaggerRoutesComponent.create().getRoutesMap()
    return routesMap[path]
  }

  enum class Paths(val path: String) {
    INDEX("/"),
    LOGIN("/login"),
    PING("/ping"),
    OIDC_CALLBACK("/oidc-callback"),
    PROFILE("/profile/@{preferred_username}"),
    DISCOURSE_LINK("/link-discourse"),
    DISCOURSE_UNLINK("/unlink-discourse"),
    DISCOURSE_CALLBACK("/discourse-callback"),
    STATIC("/static"),
    GROUPS("/groups/{group_slug}"),
    SEARCH_PRELOAD("/search-preload"),
  }
}

@Singleton
@Component(modules = [RoutesModule::class, DiscourseModule::class])
interface RoutesComponent {
  fun getRoutesMap(): Map<String, IRouteHandler>
}
