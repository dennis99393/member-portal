package org.dallasmakerspace.activedirectory

import dagger.Reusable
import javax.inject.Inject

@Reusable
class ActiveDirectoryServiceMock @Inject constructor() : IActiveDirectoryService {

  /** {@inheritDoc} */
  override fun getMemberByUsernameList(username: String): ADUser =
      sampleMembersMap[username] ?: throw ADException("User not found")

  /** {@inheritDoc} */
  override fun getMembersByUsernameList(usernameList: List<String>): Map<String, ADUser> =
      sampleMembersMap

  override fun getGroup(groupname: String): ADGroup {
    return ADGroup(
        cn = "3D Printer Basics",
        description = "Qualified to use 3D printers",
        distinguishedName = "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local",
        objectGuid = null,
        listOf(),
        false)
  }

  /** {@inheritDoc} */
  override fun getMemberByDnList(dnList: List<String>) = listOf<ADUser>()

  /** {@inheritDoc} */
  override fun getMembersByUpdatedDays(days: Int): List<ADUser> = listOf()

  companion object {
    private val sampleMembersMap =
        mapOf(
            "user1" to
                ADUser(
                    sAMAccountName = "user1",
                    givenName = "User",
                    sn = "One",
                    displayName = "User One",
                    mail = "user1@example.com",
                    objectGuid = "12345678-1234-1234-1234-123456789012",
                    whenCreated = "2021-01-01T00:00:00Z",
                    telephoneNumber = "1234567890",
                    employeeID = "123456",
                    enabled = true,
                    groups =
                        listOf(
                            ADGroup(
                                cn = "Members",
                                description = "Qualified to use 3D printers",
                                distinguishedName =
                                    "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local",
                                objectGuid = null,
                                members = listOf(),
                                membersListIncomplete = false))),
            "user2" to
                ADUser(
                    sAMAccountName = "user2",
                    givenName = "User",
                    sn = "Two",
                    displayName = "User Two",
                    mail = "user2@example.com",
                    telephoneNumber = "1234567890",
                    employeeID = "123456",
                    objectGuid = "12345678-1234-1234-1234-123456789013",
                    whenCreated = "2021-01-02T00:00:00Z",
                    enabled = false,
                    groups =
                        listOf(
                            ADGroup(
                                cn = "Members",
                                description = "Qualified to use 3D printers",
                                distinguishedName =
                                    "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local",
                                objectGuid = null,
                                members = listOf(),
                                membersListIncomplete = false))),
        )
  }
}
