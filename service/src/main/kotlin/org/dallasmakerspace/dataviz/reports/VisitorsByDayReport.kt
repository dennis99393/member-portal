package org.dallasmakerspace.dataviz.reports

import java.util.*
import javax.inject.Inject
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.db.master.GenericRepository
import org.dallasmakerspace.models.DataField
import org.dallasmakerspace.models.DataItem
import org.dallasmakerspace.models.DataType
import org.dallasmakerspace.models.DataVizResponse
import org.dallasmakerspace.models.toJsonElement
import org.dallasmakerspace.models.toJsonElementForDate

class VisitorsByDayReport @Inject constructor(private val genericRepository: GenericRepository) :
    DataVizReport() {
  override fun getName(): String {
    return "visitors-by-day"
  }

  override suspend fun getData(): DataVizResponse {
    val dbData =
        genericRepository.getReportData(
            """
          SELECT
              DATE(CONVERT_TZ(e.created, 'UTC', 'America/Chicago')) AS event_date,
            COUNT(DISTINCT e.userId) AS unique_user_count
          FROM
            `AccessControl`.events e
          WHERE
            e.created >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
          GROUP BY
            DATE(CONVERT_TZ(e.created, 'UTC', 'America/Chicago'))
          ORDER BY
            DATE(CONVERT_TZ(e.created, 'UTC', 'America/Chicago'))
        """
                .trimIndent())
    // Convert the database data to the format required by DataVizResponse
    val dataItems =
        dbData?.data?.map { row ->
          DataItem(
              values =
                  mapOf(
                      "event_date" to (row["event_date"] as Date).toString().toJsonElementForDate(),
                      "unique_user_count" to
                          (row["unique_user_count"] as Long).toInt().toJsonElement()))
        }
    val dataFields =
        listOf(
            DataField("event_date", DataType.DATE, label = "Date"),
            DataField("unique_user_count", DataType.NUMBER, label = "Unique Users"))
    return DataVizResponse(
        title = "Visitors by Date",
        description =
            "Number of members visiting DMS for the past 30 days. This data is derived from badge swipes.",
        data = dataItems,
        dataFields = dataFields)
  }
}
