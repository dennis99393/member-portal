package org.dallasmakerspace.dataviz.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.dataviz.reports.TimeOfDayReport
import org.dallasmakerspace.dataviz.reports.VisitorsByDayReport
import org.dallasmakerspace.dataviz.reports.accesscontrol.BadgeSwipesReport
import org.dallasmakerspace.dataviz.reports.calendar.CalTrendsEvents
import org.dallasmakerspace.dataviz.reports.calendar.FirstEvents
import org.dallasmakerspace.dataviz.reports.calendar.GroupCalendarReport
import org.dallasmakerspace.dataviz.reports.calendar.TopAttendees
import org.dallasmakerspace.dataviz.reports.calendar.TopOrganizers
import org.dallasmakerspace.dataviz.reports.groups.GroupHistoryReport
import org.dallasmakerspace.dataviz.reports.it.BadgeValidationAdReport
import org.dallasmakerspace.dataviz.reports.it.BadgeValidationMergedReport
import org.dallasmakerspace.dataviz.reports.it.LapsedMembersActiveAdReport
import org.dallasmakerspace.dataviz.reports.membership.ActiveMembers
import org.dallasmakerspace.dataviz.reports.membership.BadgeValidationReport
import org.dallasmakerspace.dataviz.reports.membership.DistributionDistance
import org.dallasmakerspace.dataviz.reports.membership.DistributionRate
import org.dallasmakerspace.smartwaiver.SignedWaivers

@Module
class DataVizModule {
  @Provides
  @ElementsIntoSet
  @Suppress("LongParameterList")
  fun provideReports(
      visitorsByDayReport: VisitorsByDayReport,
      timeOfDayReport: TimeOfDayReport,
      calTrendsEvents: CalTrendsEvents,
      firstEvents: FirstEvents,
      groupCalendarReport: GroupCalendarReport,
      topOrganizers: TopOrganizers,
      topAttendees: TopAttendees,
      activeMembers: ActiveMembers,
      distributionDistance: DistributionDistance,
      distributionRate: DistributionRate,
      signedWaivers: SignedWaivers,
      groupHistoryReport: GroupHistoryReport,
      badgeSwipesReport: BadgeSwipesReport,
      badgeValidationReport: BadgeValidationReport,
      badgeValidationAdReport: BadgeValidationAdReport,
      badgeValidationMergedReport: BadgeValidationMergedReport,
      lapsedMembersActiveAdReport: LapsedMembersActiveAdReport,
  ): Set<DataVizReport> {
    return setOf(
        visitorsByDayReport,
        timeOfDayReport,
        calTrendsEvents,
        firstEvents,
        groupCalendarReport,
        topOrganizers,
        topAttendees,
        activeMembers,
        distributionDistance,
        distributionRate,
        signedWaivers,
        groupHistoryReport,
        badgeSwipesReport,
        badgeValidationReport,
        badgeValidationAdReport,
        badgeValidationMergedReport,
        lapsedMembersActiveAdReport,
    )
  }
}
