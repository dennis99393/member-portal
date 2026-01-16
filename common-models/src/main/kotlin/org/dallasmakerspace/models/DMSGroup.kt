package org.dallasmakerspace.models

import kotlinx.serialization.Serializable

@Serializable
data class DMSGroup(
    val name: String,
    val description: String? = null,
    val distinguishedName: String,
    val objectGuid: String?,
    val membersListIncomplete: Boolean = false,
    val members: List<DMSMember> = emptyList(),
    val administrators: List<String> = emptyList(),
    val history: List<GroupHistory> = emptyList(),
    val nestedGroups: List<String> = emptyList(),
)
