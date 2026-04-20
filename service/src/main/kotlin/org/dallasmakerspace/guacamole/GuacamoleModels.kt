package org.dallasmakerspace.guacamole

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuacamoleTokenResponse(
    @SerialName("authToken") val authToken: String,
    @SerialName("username") val username: String,
)

@Serializable
data class GuacamoleConnection(
    @SerialName("identifier") val identifier: String,
    @SerialName("name") val name: String,
    @SerialName("protocol") val protocol: String,
)

@Serializable
data class GuacamoleActiveConnection(
    @SerialName("identifier") val identifier: String,
    @SerialName("connectionIdentifier") val connectionIdentifier: String,
    @SerialName("username") val username: String,
    @SerialName("startDate") val startDate: Long,
    @SerialName("remoteHost") val remoteHost: String = "",
)
