package org.dallasmakerspace.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.dallasmakerspace.core.AppConfig

class ApiKeyAuthProvider internal constructor(appConfig: AppConfig, authConfig: Configuration) :
    AuthenticationProvider(authConfig) {

  data class ApiKeyPrincipal(val key: String) : Principal

  private val apiClientHeaderName: String =
      requireNotNull(authConfig.apiClientHeaderName) { "authConfig.apiClientHeaderName" }
  private val apiKeyHeaderName: String =
      requireNotNull(authConfig.apiKeyHeaderName) { "authConfig.apiKeyHeaderName" }
  private val apiKeys = appConfig.requireStringProperty("app.api.keys").split(",")
  private val apiClients = appConfig.requireStringProperty("app.api.clients").split(",")

  private val mapClientsToApiKeys = apiClients.zip(apiKeys).toMap()

  private val challengeFunction = authConfig.challengeFunction

  private val authScheme = authConfig.authScheme

  override suspend fun onAuthenticate(context: AuthenticationContext) {
    val apiKey = context.call.request.header(apiKeyHeaderName)
    val apiClient = context.call.request.header(apiClientHeaderName)
    val principal =
        apiKey?.let {
          if (apiClient == null) {
            return@let null
          } else {
            if (mapClientsToApiKeys[apiClient] != apiKey) {
              return@let null
            }
            ApiKeyPrincipal(it)
          }
        }

    val cause =
        when {
          apiKey == null || apiClient == null -> AuthenticationFailedCause.NoCredentials
          principal == null -> AuthenticationFailedCause.InvalidCredentials
          else -> null
        }
    if (cause != null) {
      context.challenge(authScheme, cause) { challenge, call ->
        challengeFunction(call)

        challenge.complete()
      }
    }
    if (principal != null) {
      context.principal(principal)
    }
  }

  /** Api key auth configuration. */
  class Configuration internal constructor(name: String?) : Config(name) {

    internal lateinit var authenticationFunction: ApiKeyAuthenticationFunction

    internal var challengeFunction: ApiKeyAuthChallengeFunction = { call ->
      call.respond(HttpStatusCode.Unauthorized)
    }

    /** Name of the scheme used when challenge fails, see [AuthenticationContext.challenge]. */
    var authScheme: String = "apiKey"

    /** Name of the header that will be used as a source for the api key. */
    var apiKeyHeaderName: String = X_API_KEY

    /** Name of the header that will be used as a source for the api client. */
    var apiClientHeaderName: String = X_API_CLIENT

    /**
     * Sets a validation function that will check given API key retrieved from [apiKeyHeaderName]
     * instance and return [Principal], or null if credential does not correspond to an
     * authenticated principal.
     */
    fun validate(body: suspend ApplicationCall.(String) -> Principal?) {
      authenticationFunction = body
    }

    /** A response to send back if authentication failed. */
    fun challenge(body: ApiKeyAuthChallengeFunction) {
      challengeFunction = body
    }
  }

  companion object {
    const val X_API_KEY = "X-Api-Key"
    const val X_API_CLIENT = "X-Api-Client"
  }
}

/** Installs API Key authentication mechanism. */
fun AuthenticationConfig.apiKey(
    appConfig: AppConfig,
    name: String? = null,
    configure: ApiKeyAuthProvider.Configuration.() -> Unit
) {
  val provider =
      ApiKeyAuthProvider(appConfig, ApiKeyAuthProvider.Configuration(name).apply(configure))
  register(provider)
}

/** Alias for function signature that is invoked when verifying header. */
typealias ApiKeyAuthenticationFunction = suspend ApplicationCall.(String) -> Principal?

/** Alias for function signature that is called when authentication fails. */
typealias ApiKeyAuthChallengeFunction = suspend (ApplicationCall) -> Unit
