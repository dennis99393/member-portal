package org.dallasmakerspace.guacamole

import javax.inject.Inject
import javax.inject.Singleton
import org.dallasmakerspace.core.LoggerFactory

@Singleton
class GuacamoleApiClientMock @Inject constructor(loggerFactory: LoggerFactory) :
    IGuacamoleApiClient {
  private val log = loggerFactory.create(javaClass)

  override suspend fun getConnections(): List<GuacamoleConnection> {
    log.info("Mock: getConnections")
    return listOf(
        GuacamoleConnection(identifier = "1", name = "Mastercam Workstation", protocol = "rdp"),
        GuacamoleConnection(identifier = "2", name = "Windows 10 VM 1", protocol = "rdp"),
        GuacamoleConnection(identifier = "3", name = "Ubuntu Server 1", protocol = "ssh"),
        GuacamoleConnection(identifier = "4", name = "Win 11 VM 1", protocol = "rdp"),
    )
  }

  override suspend fun getActiveConnections(): List<GuacamoleActiveConnection> {
    log.info("Mock: getActiveConnections")
    return listOf(
        GuacamoleActiveConnection(
            identifier = "active-1",
            connectionIdentifier = "3",
            username = "jdoe",
            startDate = System.currentTimeMillis() - 300_000L,
        ))
  }

  override suspend fun killActiveConnection(identifier: String) {
    log.info("Mock: killActiveConnection identifier=$identifier")
  }

  override suspend fun getConnectionHostname(id: String): String {
    log.info("Mock: getConnectionHostname id=$id")
    return ""
  }
}
