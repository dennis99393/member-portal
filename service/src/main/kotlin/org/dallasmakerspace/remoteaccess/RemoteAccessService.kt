package org.dallasmakerspace.remoteaccess

import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.dallasmakerspace.config.ConfigOverrideService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.guacamole.GuacamoleConnection
import org.dallasmakerspace.guacamole.IGuacamoleApiClient

private const val PROBE_TIMEOUT_MS = 2_000
private const val PORT_RDP = 3389
private const val PORT_SSH = 22

@Singleton
class RemoteAccessService
@Inject
constructor(
    private val guacamoleApiClient: IGuacamoleApiClient,
    private val configOverrideService: ConfigOverrideService,
    loggerFactory: LoggerFactory,
) {
  private val log = loggerFactory.create(javaClass)

  suspend fun killActiveConnection(identifier: String) {
    guacamoleApiClient.killActiveConnection(identifier)
  }

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

    // Collect all unique connection IDs across all categories
    val allConnectionIds =
        RemoteAccessCategory.entries
            .flatMap { category ->
              configOverrideService
                  .get(category.connectionIdsConfigKey)
                  .split(",")
                  .map { it.trim() }
                  .filter { it.isNotEmpty() }
            }
            .distinct()

    // Launch TCP probes in parallel for all known connections
    val probeJobs =
        allConnectionIds.mapNotNull { id ->
          connectionById[id]?.let { conn -> id to async { probeConnectivity(conn) } }
        }
    // All probes are running concurrently; await each in turn
    val reachabilityById = buildMap<String, Boolean> {
      for ((id, deferred) in probeJobs) put(id, deferred.await())
    }

    RemoteAccessCategory.entries.map { category ->
      val connectionIdsRaw = configOverrideService.get(category.connectionIdsConfigKey)
      val requiredAdGroup = configOverrideService.get(category.adGroupConfigKey)
      val connectionIds =
          connectionIdsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

      val machines =
          connectionIds.mapNotNull { id ->
            val conn = connectionById[id]
            if (conn == null) {
              log.warn("Guacamole connection ID $id (category ${category.slug}) not found")
              null
            } else {
              val reachable = reachabilityById[id] ?: true
              val sessions = activeSessions[id]
              val firstSession = sessions?.firstOrNull()
              val status =
                  when {
                    !reachable -> MachineStatus.OFFLINE
                    !sessions.isNullOrEmpty() -> MachineStatus.IN_USE
                    else -> MachineStatus.AVAILABLE
                  }
              RemoteAccessMachineDTO(
                  connectionId = id,
                  displayName = conn.name,
                  protocol = conn.protocol,
                  status = status,
                  occupantUsername = firstSession?.username,
                  sessionStartEpochMs = firstSession?.startDate,
                  activeConnectionId = firstSession?.identifier,
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

  private suspend fun probeConnectivity(conn: GuacamoleConnection): Boolean {
    val host = conn.parameters.hostname
    if (host.isBlank()) return true
    val port = if (conn.protocol == "rdp") PORT_RDP else PORT_SSH
    return withContext(Dispatchers.IO) {
      try {
        Socket().use { socket ->
          socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
          true
        }
      } catch (e: Exception) {
        log.debug("VM {} ({}:{}) unreachable: {}", conn.name, host, port, e.message)
        false
      }
    }
  }
}
