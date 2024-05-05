package org.dallasmakerspace.thymeleaf.server.routes

import dagger.Component
import io.ktor.server.application.*
import io.ktor.server.request.*
import javax.inject.Singleton
import org.dallasmakerspace.thymeleaf.server.routes.di.RoutesModule

object RouteFactory {

  fun getHandler(call: ApplicationCall): IRouteHandler? {
    val path = call.request.uri
    return getHandler(path)
  }

  fun getHandler(path: String): IRouteHandler? {
    val routesMap = DaggerRoutesComponent.create().getRoutesMap()
    return routesMap[path]
  }

  enum class Paths(val path: String) {
    INDEX("/"),
    LOGIN("/login"),
    OIDC_CALLBACK("/oidc-callback"),
    PROFILE("/profile"),
    STATIC("/static"),
  }
}

@Singleton
@Component(modules = [RoutesModule::class])
interface RoutesComponent {
  fun getRoutesMap(): Map<String, IRouteHandler>
}
