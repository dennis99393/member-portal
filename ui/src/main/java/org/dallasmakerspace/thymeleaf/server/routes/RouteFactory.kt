package org.dallasmakerspace.thymeleaf.server.routes

import dagger.Component
import io.ktor.server.application.*
import io.ktor.server.request.*
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.di.DiscourseModule
import org.dallasmakerspace.thymeleaf.server.di.RoutesModule

object RouteFactory {
  val redirects = mutableMapOf<String, String>()

  fun getHandler(call: ApplicationCall): IRouteHandler? {
    val path = sanitizePathInternal(call.request.uri)
    return getHandler(path)
  }

  private fun sanitizePathInternal(path: String): String {
    // Replace "~.*" with "~{preferred_username}"
    return path.replace("~.*".toRegex(), "~{preferred_username}")
  }

  fun getHandler(path: String): IRouteHandler? {
    val routesMap = DaggerRoutesComponent.create().getRoutesMap()
    return routesMap[path]
  }

  enum class Paths(val path: String) {
    INDEX("/"),
    LOGIN("/login"),
    OIDC_CALLBACK("/oidc-callback"),
    PROFILE("/profile/~{preferred_username}"),
    DISCOURSE_LINK("/link-discourse"),
    DISCOURSE_UNLINK("/unlink-discourse"),
    DISCOURSE_CALLBACK("/discourse-callback"),
    STATIC("/static"),
  }
}

@Singleton
@Component(modules = [RoutesModule::class, DiscourseModule::class])
interface RoutesComponent {
  fun getRoutesMap(): Map<String, IRouteHandler>
}
