package org.dallasmakerspace.server.reports

object ReportGraph {
  @JvmStatic
  fun getRoot(): ReportNode {
    return ReportNode(
        name = "root",
        description = "Root node of the report graph",
        urlSlug = "reports",
        children =
            listOf(
                ReportNode(
                    name = "Calendar",
                    description = "Calendar reports",
                    urlSlug = "calendar",
                    children =
                        listOf(
                            ReportNode(
                                name = "Event Trends",
                                description = "Calendar trends",
                                urlSlug = "event-trends",
                            ),
                        ),
                ),
                ReportNode(
                    name = "Member Visits",
                    description = "Reports generated from badge swipes",
                    urlSlug = "member-visits",
                    children =
                        listOf(
                            ReportNode(
                                name = "Visitors by Date",
                                description = "Number of badge swipes per day",
                                urlSlug = "daily-activity",
                            ),
                            ReportNode(
                                name = "Time of Day",
                                description = "Average activity by time of day",
                                urlSlug = "time-of-day",
                            ),
                        )),
            ))
  }
}

data class ReportNode(
    val name: String,
    val description: String,
    val children: List<ReportNode>? = null,
    val urlSlug: String,
)
