package org.dallasmakerspace.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.dallasmakerspace.core.AppConfig
import org.slf4j.LoggerFactory

class ApiKeyAuthProvider internal constructor(appConfig: AppConfig, authConfig: Configuration) :
    AuthenticationProvider(authConfig) {

  private val log = LoggerFactory.getLogger(ApiKeyAuthProvider::class.java)

  data class ApiKeyPrincipal(val key: String, val client: String, val roles: Set<String>) :
      Principal

  private val apiClientHeaderName: String =
      requireNotNull(authConfig.apiClientHeaderName) { "authConfig.apiClientHeaderName" }
  private val apiKeyHeaderName: String =
      requireNotNull(authConfig.apiKeyHeaderName) { "authConfig.apiKeyHeaderName" }

  // Data class to hold API client configuration
  private data class ApiClientConfig(val name: String, val key: String, val roles: Set<String>)

  // Load API clients from indexed environment variables
  // Format: API_CLIENT_0_NAME, API_CLIENT_0_KEY, API_CLIENT_0_ROLES, etc.
  private val apiClients: List<ApiClientConfig> = run {
    val clientCount =
        try {
          appConfig.requireStringProperty("app.api.client-count").toInt()
        } catch (e: Exception) {
          log.warn("API client count not configured (app.api.client-count), defaulting to 0")
          0
        }

    val loadedClients =
        (0 until clientCount).mapNotNull { i ->
          val name = System.getenv("API_CLIENT_${i}_NAME")
          val key = System.getenv("API_CLIENT_${i}_KEY")
          val rolesString = System.getenv("API_CLIENT_${i}_ROLES")

          when {
            name.isNullOrBlank() -> {
              log.warn("API_CLIENT_${i}_NAME is not set or empty, skipping client at index $i")
              null
            }
            key.isNullOrBlank() -> {
              log.warn("API_CLIENT_${i}_KEY is not set or empty for client '$name', skipping")
              null
            }
            else -> {
              val roles =
                  rolesString?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                      ?: emptySet()

              if (roles.isEmpty()) {
                log.warn("No roles configured for client '$name' (API_CLIENT_${i}_ROLES)")
              } else {
                log.info(
                    "Loaded client '$name' with ${roles.size} role(s): ${roles.joinToString(", ")}"
                )
              }

              ApiClientConfig(name, key, roles)
            }
          }
        }

    // If no clients were loaded from env vars, use fallback from config (for local dev)
    loadedClients.ifEmpty {
      log.warn("No API clients loaded from environment variables, using fallback configuration")
      try {
        val fallbackName = appConfig.requireStringProperty("app.api.fallback.name")
        val fallbackKey = appConfig.requireStringProperty("app.api.fallback.key")
        val fallbackRoles =
            appConfig
                .requireStringProperty("app.api.fallback.roles")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()

        log.info(
            "Loaded fallback client '$fallbackName' with ${fallbackRoles.size} role(s): ${
            fallbackRoles.joinToString(
              ", "
            )
          }"
        )
        listOf(ApiClientConfig(fallbackName, fallbackKey, fallbackRoles))
      } catch (e: Exception) {
        log.error("Failed to load fallback client configuration: ${e.message}")
        emptyList()
      }
    }
  }

  private val mapClientsToApiKeys = apiClients.associate { it.name to it.key }
  private val mapClientsToRoles = apiClients.associate { it.name to it.roles }

  private val challengeFunction = authConfig.challengeFunction

  private val authScheme = authConfig.authScheme

  override suspend fun onAuthenticate(context: AuthenticationContext) {
    val apiKey = context.call.request.header(apiKeyHeaderName)
    val apiClient = context.call.request.header(apiClientHeaderName)

    val principal =
        apiKey?.let {
          if (apiClient == null) {
            log.warn(
                "Authentication failed: Missing client header. " +
                    "Key prefix: ${maskApiKey(it)}, Client: <not provided>"
            )
            return@let null
          } else {
            if (mapClientsToApiKeys[apiClient] != apiKey) {
              log.warn(
                  "Authentication failed: Invalid credentials. " +
                      "Key prefix: ${maskApiKey(it)}, Client: '$apiClient'"
              )
              return@let null
            }
            // Create principal with client name and roles
            val roles = mapClientsToRoles[apiClient] ?: emptySet()
            log.info(
                "Authentication successful for client: '$apiClient' with ${roles.size} role(s)"
            )
            ApiKeyPrincipal(it, apiClient, roles)
          }
        }

    val cause =
        when {
          apiKey == null && apiClient == null -> {
            log.warn("Authentication failed: Missing both API key and client headers")
            AuthenticationFailedCause.NoCredentials
          }
          apiKey == null -> {
            log.warn(
                "Authentication failed: Missing API key header. Client: '${apiClient ?: "<none>"}'"
            )
            AuthenticationFailedCause.NoCredentials
          }
          apiClient == null -> {
            // Already logged above in the let block
            AuthenticationFailedCause.NoCredentials
          }
          principal == null -> {
            // Already logged above in the let block
            AuthenticationFailedCause.InvalidCredentials
          }
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

  /**
   * Masks an API key for safe logging by showing only the first 4 characters. Example:
   * "my-secret-key-123" becomes "my-s***"
   */
  private fun maskApiKey(key: String): String {
    return if (key.length <= 4) {
      "***"
    } else {
      "${key.take(4)}***"
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
    configure: ApiKeyAuthProvider.Configuration.() -> Unit,
) {
  val provider =
      ApiKeyAuthProvider(appConfig, ApiKeyAuthProvider.Configuration(name).apply(configure))
  register(provider)
}

/** Alias for function signature that is invoked when verifying header. */
typealias ApiKeyAuthenticationFunction = suspend ApplicationCall.(String) -> Principal?

/** Alias for function signature that is called when authentication fails. */
typealias ApiKeyAuthChallengeFunction = suspend (ApplicationCall) -> Unit
