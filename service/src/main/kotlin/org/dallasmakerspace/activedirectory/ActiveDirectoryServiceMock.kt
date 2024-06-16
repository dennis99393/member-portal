package org.dallasmakerspace.activedirectory

import dagger.Reusable
import javax.inject.Inject

@Reusable
class ActiveDirectoryServiceMock @Inject constructor() : IActiveDirectoryService {
  /** {@inheritDoc} */
  override fun getMember(username: String): ADUser {
    return sampleMembersMap[username] ?: throw ADException("User not found")
  }

  companion object {
    private val sampleMembersMap =
        mapOf(
            "user1" to
                ADUser(
                    cn = "user1",
                    firstName = "User",
                    lastName = "One",
                    displayName = "User One",
                    mail = "user1@example.com",
                    objectGuid = "12345678-1234-1234-1234-123456789012",
                    groups =
                        listOf(
                            ADGroup(
                                cn = "Members",
                                distinguishedName =
                                    "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local",
                                objectGuid = null))),
            "user2" to
                ADUser(
                    cn = "user2",
                    firstName = "User",
                    lastName = "Two",
                    displayName = "User Two",
                    mail = "user2@example.com",
                    objectGuid = "12345678-1234-1234-1234-123456789013",
                    groups =
                        listOf(
                            ADGroup(
                                cn = "Members",
                                distinguishedName =
                                    "cn=Members,ou=Security,ou=Groups,dc=dms,dc=local",
                                objectGuid = null))),
        )
  }
}
