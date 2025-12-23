package org.dallasmakerspace.server.routes

import dagger.Component
import io.ktor.server.application.*
import io.ktor.server.request.*
import javax.inject.Singleton
import org.dallasmakerspace.server.di.DiscourseModule
import org.dallasmakerspace.server.di.RoutesModule
import org.dallasmakerspace.server.di.VoterRegistrationModule

object RouteFactory {
  val redirects = mutableMapOf<String, String>()

  fun getHandler(call: ApplicationCall): IRouteHandler? {
    val path = sanitizePathInternal(call.request.uri)
    return getHandler(path)
  }

  private fun sanitizePathInternal(path: String): String {
    // Replace "@.*" with "@{preferred_username}"
    return path
        .replace("@[a-zA-Z0-9_-]*".toRegex(), "@{preferred_username}")
        .replace("/groups/.*".toRegex(), "/groups/{group_slug}")
        .replace("/reports.*".toRegex(), "/reports/{path...}")
        .replace("/backend-api/.*".toRegex(), "/backend-api/{path...}")
        .replace("/go/.*".toRegex(), "/go/{path...}")
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
    PROFILEME("/profile-me"),
    DISCOURSE_LINK("/link-discourse"),
    DISCOURSE_UNLINK("/unlink-discourse"),
    DISCOURSE_CALLBACK("/discourse-callback"),
    STATIC("/static"),
    GROUPS("/groups/{group_slug}"),
    SEARCH_PRELOAD("/search-preload"),
    REGISTER_VOTING("/profile/@{preferred_username}/register-voting"),
    UNREGISTER_VOTING("/profile/@{preferred_username}/unregister-voting"),
    REPORT("/reports/{path...}"),
    BACKEND_API("/backend-api/{path...}"),
    SHORT_LINKS("/short-links"),
    SHORT_LINKS_ADMIN("/short-links/admin"),
    SHORT_LINK_REDIRECT("/go/{path...}"),
    COMMITTEES("/committees"),
  }
}

@Singleton
@Component(modules = [RoutesModule::class, DiscourseModule::class, VoterRegistrationModule::class])
interface RoutesComponent {
  fun getRoutesMap(): Map<String, IRouteHandler>
}
