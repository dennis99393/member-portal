package org.dallasmakerspace.server.auth

class Authz(private val permissions: Set<Permission>) {
    fun can(permissionName: String): Boolean = permissions.any { it.name == permissionName }
}
