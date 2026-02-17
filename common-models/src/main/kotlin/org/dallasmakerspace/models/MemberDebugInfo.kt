package org.dallasmakerspace.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** Data class to hold member debug information visible to infrastructure team. */
@Serializable
data class MemberDebugInfo(
    /** Whether the AD account is enabled (from LDAP userAccountControl attribute) */
    val adAccountEnabled: Boolean,
    /** Whether MM considers the AD account active (from MakerManager users.ad_active field) */
    val mmAdActive: Boolean,
    /** Whether the member currently has an active WHMCS product */
    val whmcsActive: Boolean,
    /**
     * Number of days in the current WHMCS status. If active, days since last went active. If
     * inactive, days since last went inactive.
     */
    val daysInCurrentWhmcsStatus: Int?,
    /** Total number of days member had active WHMCS products (sum of all ACTIVE periods) */
    val totalActiveDays: Int?,
    /** Unified chronological timeline of all active periods and gaps */
    val timeline: List<TimelineEntryInfo>
)

/** Represents an entry in the product coverage timeline (either active period or gap) */
@Serializable
data class TimelineEntryInfo(
    val type: TimelineEntryType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val durationDays: Int
)

@Serializable
enum class TimelineEntryType {
  ACTIVE,
  GAP
}
