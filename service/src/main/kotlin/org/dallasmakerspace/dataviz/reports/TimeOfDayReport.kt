package org.dallasmakerspace.dataviz.reports

import java.math.BigDecimal
import javax.inject.Inject
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.db.master.GenericRepository
import org.dallasmakerspace.models.DataField
import org.dallasmakerspace.models.DataItem
import org.dallasmakerspace.models.DataType
import org.dallasmakerspace.models.DataVizResponse
import org.dallasmakerspace.models.toJsonElement

class TimeOfDayReport @Inject constructor(private val genericRepository: GenericRepository) :
    DataVizReport() {
  override fun getName(): String {
    return "time-of-day"
  }

  override suspend fun getData(): DataVizResponse {
    val dbData = genericRepository.getReportData(QUERY.trimIndent())
    // Convert the database data to the format required by DataVizResponse
    val dataItems =
        dbData?.data?.map { row ->
          DataItem(
              values =
                  mapOf(
                      "Day" to (row["Day"] as String).toJsonElement(),
                      "Time Slot" to (row["Time Slot"] as String).toJsonElement(),
                      "Average Activity" to
                          (row["Average Activity"] as BigDecimal).toInt().toJsonElement()))
        }
    val dataFields =
        listOf(
            DataField("Day", DataType.STRING),
            DataField("Time Slot", DataType.STRING),
            DataField("Average Activity", DataType.NUMBER))
    return DataVizResponse(
        title = "Activity by Time of Day",
        description =
            "Average number of members visiting DMS for the past 90 days grouped by the time of day. " +
                "This data is derived from badge swipes.",
        data = dataItems,
        dataFields = dataFields)
  }

  private companion object {
    const val QUERY =
        """
          SELECT 
              Day,
              `Time Slot`,
              ROUND(AVG(daily_unique_users), 0) AS `Average Activity`
          FROM (
              SELECT 
                  DAYNAME(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) AS Day,
                  DAYOFWEEK(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) AS day_num,
                  DATE(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) AS event_date,
                  CASE 
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 7 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 12 
                      THEN '7AM - 12PM'
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 12 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 16.5 
                      THEN '12PM - 4:30PM'
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 16.5 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 21 
                      THEN '4:30PM - 9PM'
                      ELSE 'Other Hours'
                  END AS `Time Slot`,
                  COUNT(DISTINCT e.userId) AS daily_unique_users
              FROM 
                  `AccessControl`.events e
              WHERE 
                  e.created >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
                  AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) BETWEEN 7 AND 20
              GROUP BY 
                  event_date,
                  DAYOFWEEK(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')),
                  DAYNAME(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')),
                  CASE 
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 7 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 12 
                      THEN '7AM - 12PM'
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 12 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 16.5 
                      THEN '12PM - 4:30PM'
                      WHEN HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) >= 16.5 
                           AND HOUR(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) < 21 
                      THEN '4:30PM - 9PM'
                      ELSE 'Other Hours'
                  END
          ) daily_stats
          GROUP BY 
              day_num,
              Day,
              `Time Slot`
          ORDER BY 
              day_num,
              CASE 
                  WHEN `Time Slot` = '7AM - 12PM' THEN 1
                  WHEN `Time Slot` = '12PM - 4:30PM' THEN 2
                  WHEN `Time Slot` = '4:30PM - 9PM' THEN 3
                  ELSE 4
              END;
          """
  }
}
