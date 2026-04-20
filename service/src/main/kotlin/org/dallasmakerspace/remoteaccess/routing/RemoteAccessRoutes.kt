package org.dallasmakerspace.remoteaccess.routing

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dallasmakerspace.plugins.ApiResponse
import org.dallasmakerspace.plugins.Status
import org.dallasmakerspace.remoteaccess.RemoteAccessService

fun Route.remoteAccessRoutes(remoteAccessService: RemoteAccessService) {
  get("/remote-access/categories") {
    val categories = remoteAccessService.getCategories()
    call.respond(
        ApiResponse(Status.SUCCESS, "Remote access categories: ${categories.size}", categories))
  }
}
