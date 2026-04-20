package org.dallasmakerspace.remoteaccess

import kotlinx.serialization.Serializable

@Serializable
data class RemoteAccessMachineDTO(
    val connectionId: String,
    val displayName: String,
    val protocol: String,
    val inUse: Boolean,
    val occupantUsername: String?,
    val sessionStartEpochMs: Long?,
    val activeConnectionId: String?,
)

@Serializable
data class RemoteAccessCategoryDTO(
    val slug: String,
    val displayName: String,
    val description: String,
    val requiredAdGroup: String,
    val machines: List<RemoteAccessMachineDTO>,
)
