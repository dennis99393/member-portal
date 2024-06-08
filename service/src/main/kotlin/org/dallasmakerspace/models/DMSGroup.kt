package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class DMSGroup(
    val name: String,
    val distinguishedName: String,
    val objectGuid: String?,
    val members: List<DMSMember>?
)
