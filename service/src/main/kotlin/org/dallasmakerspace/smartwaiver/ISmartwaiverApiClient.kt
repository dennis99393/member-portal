package org.dallasmakerspace.smartwaiver

import java.time.LocalDate

interface ISmartwaiverApiClient {
  /**
   * Get a list of waivers signed within a date range.
   *
   * @param fromDate The start date (inclusive)
   * @param toDate The end date (inclusive)
   * @return List of waiver signing data
   */
  suspend fun getWaivers(fromDate: LocalDate, toDate: LocalDate): List<WaiverSigningData>

  suspend fun getWaiverDetails(fromDate: LocalDate, toDate: LocalDate): List<SmartwaiverSummary>
}
