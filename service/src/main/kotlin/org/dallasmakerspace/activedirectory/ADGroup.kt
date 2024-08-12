package org.dallasmakerspace.activedirectory

data class ADGroup(
    val cn: String,
    val description: String?,
    val distinguishedName: String,
    val objectGuid: String?,
    val members: List<ADUser>,
    val membersListIncomplete: Boolean
)
