package org.dallasmakerspace.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

/**
 * Plugin that performs role-based authorization by checking if the authenticated
 * principal has at least one of the required roles.
 */
class RoleBasedAuthorizationPlugin(private val requiredRoles: Set<String>) {

    class Configuration {
        internal val requiredRoles = mutableSetOf<String>()

        /**
         * Add a required role. The authenticated principal must have at least ONE
         * of the specified roles to access the route.
         */
        fun role(role: String) {
            requiredRoles.add(role)
        }

        /**
         * Add multiple required roles at once.
         */
        fun roles(vararg roles: String) {
            requiredRoles.addAll(roles)
        }
    }

    companion object Plugin : BaseRouteScopedPlugin<Configuration, RoleBasedAuthorizationPlugin> {

        override val key = AttributeKey<RoleBasedAuthorizationPlugin>("RoleBasedAuthorization")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): RoleBasedAuthorizationPlugin {
            val config = Configuration().apply(configure)
            val plugin = RoleBasedAuthorizationPlugin(config.requiredRoles.toSet())

            // Intercept requests and validate roles
            // Use Call phase to run AFTER authentication (which happens in Plugins phase)
            pipeline.intercept(ApplicationCallPipeline.Call) {
                val principal = call.principal<ApiKeyAuthProvider.ApiKeyPrincipal>()

                // If no principal, user is not authenticated
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Authentication required")
                    finish()
                    return@intercept
                }

                // Check if principal has at least one of the required roles
                val hasRequiredRole = plugin.requiredRoles.any { it in principal.roles }

                if (!hasRequiredRole) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Client '${principal.client}' lacks required roles: ${plugin.requiredRoles.joinToString(", ")}"
                    )
                    finish()
                    return@intercept
                }

                // Principal has required role, continue with request
            }

            return plugin
        }
    }
}

/**
 * Extension function to apply role-based authorization to a route.
 *
 * Usage:
 * ```
 * authenticate(ApiKeyAuthProvider.X_API_KEY) {
 *     requireRoles("member:read", "member:write") {
 *         get<Members> { ... }
 *         patch<Members.DMSMember.Update> { ... }
 *     }
 * }
 * ```
 */
fun Route.requireRoles(vararg roles: String, build: Route.() -> Unit): Route {
    val authorizedRoute = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
            RouteSelectorEvaluation.Constant
    })

    authorizedRoute.install(RoleBasedAuthorizationPlugin) {
        roles(*roles)
    }

    authorizedRoute.apply(build)
    return authorizedRoute
}

/**
 * Extension function to apply single role requirement.
 */
fun Route.requireRole(role: String, build: Route.() -> Unit): Route {
    return requireRoles(role, build = build)
}
