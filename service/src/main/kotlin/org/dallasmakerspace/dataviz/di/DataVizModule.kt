package org.dallasmakerspace.dataviz.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.dataviz.reports.TimeOfDayReport
import org.dallasmakerspace.dataviz.reports.VisitorsByDayReport
import org.dallasmakerspace.dataviz.reports.calendar.CalTrendsEvents
import org.dallasmakerspace.dataviz.reports.calendar.FirstEvents
import org.dallasmakerspace.dataviz.reports.membership.ActiveMembers

@Module
class DataVizModule {
  @Provides
  @ElementsIntoSet
  fun provideReports(
      visitorsByDayReport: VisitorsByDayReport,
      timeOfDayReport: TimeOfDayReport,
      calTrendsEvents: CalTrendsEvents,
      firstEvents: FirstEvents,
      activeMembers: ActiveMembers,
  ): Set<DataVizReport> {
    return setOf(visitorsByDayReport, timeOfDayReport, calTrendsEvents, firstEvents, activeMembers)
  }
}
