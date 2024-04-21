package org.dallasmakerspace.thymeleaf.data

import io.ktor.server.auth.Principal
import kotlinx.serialization.Serializable

@Serializable
data class ADPrincipalUser(
    val accessToken: String,
    val sub: String,
    val emailVerified: Boolean,
    val name: String,
    val groups: List<String>,
    val preferredUsername: String,
    val employeeid: String,
    val email: String,
) : Principal
/*
fun toMap(): Map<String, Any> {
  return mapOf(
    "sub" to sub,
    "email_verified" to emailVerified,
    "name" to name,
    "groups" to groups,
    "preferred_username" to preferredUsername,
    "employeeid" to employeeid,
    "email" to email,
  )
}*/
