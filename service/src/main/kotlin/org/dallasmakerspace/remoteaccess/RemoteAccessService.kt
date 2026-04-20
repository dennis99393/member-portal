package org.dallasmakerspace.remoteaccess

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.dallasmakerspace.config.ConfigOverrideService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.guacamole.IGuacamoleApiClient

@Singleton
class RemoteAccessService
@Inject
constructor(
    private val guacamoleApiClient: IGuacamoleApiClient,
    private val configOverrideService: ConfigOverrideService,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun getCategories(): List<RemoteAccessCategoryDTO> = coroutineScope {
    val connectionsDeferred = async {
      try {
        guacamoleApiClient.getConnections()
      } catch (e: Exception) {
        log.warn("Failed to fetch Guacamole connections", e)
        emptyList()
      }
    }
    val activeConnectionsDeferred = async {
      try {
        guacamoleApiClient.getActiveConnections()
      } catch (e: Exception) {
        log.warn("Failed to fetch Guacamole active connections", e)
        emptyList()
      }
    }

    val connections = connectionsDeferred.await()
    val activeConnections = activeConnectionsDeferred.await()

    val connectionById = connections.associateBy { it.identifier }
    val activeSessions = activeConnections.groupBy { it.connectionIdentifier }

    RemoteAccessCategory.entries.map { category ->
      val connectionIdsRaw = configOverrideService.get(category.connectionIdsConfigKey)
      val requiredAdGroup = configOverrideService.get(category.adGroupConfigKey)

      val connectionIds = connectionIdsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

      val machines =
          connectionIds.mapNotNull { id ->
            val conn = connectionById[id]
            if (conn == null) {
              log.warn("Guacamole connection ID $id (category ${category.slug}) not found")
              null
            } else {
              val sessions = activeSessions[id]
              val inUse = !sessions.isNullOrEmpty()
              val firstSession = sessions?.firstOrNull()
              RemoteAccessMachineDTO(
                  connectionId = id,
                  displayName = conn.name,
                  protocol = conn.protocol,
                  inUse = inUse,
                  occupantUsername = firstSession?.username,
                  sessionStartEpochMs = firstSession?.startDate,
              )
            }
          }

      RemoteAccessCategoryDTO(
          slug = category.slug,
          displayName = category.displayName,
          description = category.description,
          requiredAdGroup = requiredAdGroup,
          machines = machines,
      )
    }
  }
}
