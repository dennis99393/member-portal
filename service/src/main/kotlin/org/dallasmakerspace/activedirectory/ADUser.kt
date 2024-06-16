package org.dallasmakerspace.activedirectory

data class ADUser(
    val cn: String,
    val mail: String,
    val firstName: String?,
    val lastName: String?,
    val displayName: String?,
    val objectGuid: String,
    val groups: List<ADGroup>
)
