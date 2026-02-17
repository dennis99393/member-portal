package org.dallasmakerspace.dataviz.reports.it

import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.dallasmakerspace.activedirectory.ActiveDirectoryClient
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.db.master.GenericRepository
import org.dallasmakerspace.db.master.WhmcsDataService
import org.dallasmakerspace.members.MemberService
import org.dallasmakerspace.models.DataField
import org.dallasmakerspace.models.DataItem
import org.dallasmakerspace.models.DataType
import org.dallasmakerspace.models.DataVizResponse

class LapsedMembersActiveAdReport
@Inject
constructor(
    private val adClient: ActiveDirectoryClient,
    private val memberService: MemberService,
    private val genericRepository: GenericRepository,
    private val whmcsDataService: WhmcsDataService,
) : DataVizReport() {
  override fun getName(): String {
    return "lapsed-members-active-ad"
  }

  override suspend fun getData(params: Map<String, List<String>>): DataVizResponse {
    val startTime = System.currentTimeMillis()

    // Query all AD users with badges
    val allAdUsers = adClient.getAllActiveUsersWithBadges()

    // Filter for users with badge "0000000" and enabled accounts
    val lapsedMembers =
        allAdUsers
            .filter { (_, attributes) ->
              val badgeNumber = attributes["employeeID"] as? String ?: ""
              val userAccountControl =
                  (attributes["userAccountControl"] as? String)?.toIntOrNull() ?: 0
              val isActive =
                  (userAccountControl and 0x0002) == 0 // Check if account is NOT disabled

              // Look for badge "0000000" (7 zeros) and active account
              badgeNumber == "0000000" && isActive
            }
            .toList()

    // Collect usernames for member service lookup
    val usernames = lapsedMembers.map { it.first }

    // Query MakerManager for ad_active status
    val mmAdActiveStatus = queryMakerManagerAdActiveStatus(usernames)

    // Batch fetch member profiles
    val members = memberService.getMembersByUsernameList(usernames)

    // Extract WHMCS IDs from members' account info
    val whmcsIds =
        members.values
            .mapNotNull { member ->
              val accountInfo = member.accountInfo
              if (accountInfo != null) {
                val account =
                    if (accountInfo.isPrimaryAccount) accountInfo.primaryAccount
                    else accountInfo.addonAccounts.find { it.username == member.username }
                account?.whmcsId
              } else {
                null
              }
            }
            .distinct()

    // Batch query WHMCS account status (only if we have IDs to query)
    val whmcsAccountStatuses =
        if (whmcsIds.isNotEmpty()) {
          whmcsDataService.getAccountInfoMap(whmcsIds)
        } else {
          emptyMap()
        }

    // Transform to DataItems
    val dataItems =
        lapsedMembers.map { (username, attributes) ->
          val displayName = attributes["displayName"] as? String ?: username
          val userAccountControl = (attributes["userAccountControl"] as? String)?.toIntOrNull() ?: 0
          val adEnabled = (userAccountControl and 0x0002) == 0
          val mmAdActive = mmAdActiveStatus[username] ?: false
          val member = members[username]

          // Determine WHMCS active status
          val accountInfo = member?.accountInfo
          val whmcsId =
              if (accountInfo != null) {
                val account =
                    if (accountInfo.isPrimaryAccount) accountInfo.primaryAccount
                    else accountInfo.addonAccounts.find { it.username == username }
                account?.whmcsId
              } else {
                null
              }
          val whmcsActive =
              if (whmcsId != null) {
                whmcsAccountStatuses[whmcsId]?.wasActiveInRange ?: false
              } else {
                false
              }

          val avatarUrl =
              member
                  ?.discourseAvatarUrl
                  ?.takeIf { it.isNotEmpty() }
                  ?.let { url ->
                    when {
                      url.startsWith("//") -> "https:$url"
                      else -> "https://talk.dallasmakerspace.org$url"
                    }.replace("{size}", "144")
                  } ?: ""

          // Build member JSON object
          val memberJson = buildJsonObject {
            put("username", JsonPrimitive(username))
            put("displayName", JsonPrimitive(member?.displayName ?: displayName))
            put("avatarUrl", JsonPrimitive(avatarUrl))
          }

          DataItem(
              values =
                  mapOf(
                      "Member" to memberJson,
                      "AD Account Enabled" to createStatusBadge(adEnabled),
                      "MM AD Active" to createStatusBadge(mmAdActive),
                      "WHMCS Active" to createStatusBadge(whmcsActive),
                  ))
        }

    val timeTaken = System.currentTimeMillis() - startTime
    val metadata =
        mapOf(
            "Generated at" to java.time.Instant.now().toString(),
            "Time taken" to "${timeTaken}ms",
            "Total lapsed members" to "${dataItems.size}",
            "Source" to "Active Directory (LDAP)",
            "Description" to
                "Members with badge number 0000000 and enabled AD accounts (typically for temporary email/file access)",
        )

    return DataVizResponse(
        data = dataItems,
        dataFields = getDataFields(),
        metadata = metadata,
    )
  }

  /**
   * Queries MakerManager database for ad_active status of given users.
   *
   * @param usernames List of usernames to query
   * @return Map of username to ad_active status (true/false)
   */
  private suspend fun queryMakerManagerAdActiveStatus(
      usernames: List<String>
  ): Map<String, Boolean> {
    if (usernames.isEmpty()) return emptyMap()

    // Build OR conditions for username matching
    val usernameConditions = usernames.joinToString(" OR ") { "u.username = '$it'" }

    val result =
        genericRepository.getReportData(
            """
            SELECT u.username, u.ad_active
            FROM `dms-makermanager`.users u
            WHERE $usernameConditions
            """
                .trimIndent())

    return result
        ?.data
        ?.associate { row ->
          val username = row["username"]?.toString()?.removeSurrounding("\"")
          val adActive =
              when (val value = row["ad_active"]) {
                is Number -> value.toInt() == 1
                is Boolean -> value
                is String -> value == "1" || value.lowercase() == "true"
                else -> false
              }
          username to adActive
        }
        ?.filterKeys { it != null } as? Map<String, Boolean> ?: emptyMap()
  }

  private fun getDataFields(): List<DataField> {
    return listOf(
        DataField(name = "Member", type = DataType.MEMBER, label = "Member"),
        DataField(name = "AD Account Enabled", type = DataType.BADGE, label = "AD Account Enabled"),
        DataField(name = "MM AD Active", type = DataType.BADGE, label = "MM AD Active"),
        DataField(name = "WHMCS Active", type = DataType.BADGE, label = "WHMCS Active"),
    )
  }

  /**
   * Creates a badge JSON object for status display.
   *
   * @param isActive Whether the status is active
   * @return JsonObject with variant (success/danger) and text (Active/Inactive)
   */
  private fun createStatusBadge(isActive: Boolean): JsonObject {
    return buildJsonObject {
      put("variant", JsonPrimitive(if (isActive) "success" else "danger"))
      put("text", JsonPrimitive(if (isActive) "Active" else "Inactive"))
    }
  }
}
