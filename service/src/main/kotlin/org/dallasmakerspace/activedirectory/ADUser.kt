package org.dallasmakerspace.activedirectory

data class ADUser(
    val sAMAccountName: String,
    val mail: String,
    val givenName: String?,
    val sn: String?,
    val displayName: String?,
    val objectGuid: String,
    val whenCreated: String?,
    val groups: List<ADGroup>
)
