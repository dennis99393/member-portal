package org.dallasmakerspace.dataviz.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.dataviz.DataVizRouter
import org.dallasmakerspace.dataviz.reports.TimeOfDayReport
import org.dallasmakerspace.dataviz.reports.VisitorsByDayReport
import org.dallasmakerspace.db.master.GenericRepository

@Module
class DataVizModule {

  @Provides
  fun provideVisitorsByDayReport(genericRepository: GenericRepository): VisitorsByDayReport {
    return VisitorsByDayReport(genericRepository)
  }

  @Provides
  fun provideTimeOfDayReport(genericRepository: GenericRepository): TimeOfDayReport {
    return TimeOfDayReport(genericRepository)
  }

  @Provides
  @ElementsIntoSet
  fun provideReports(
      visitorsByDayReport: VisitorsByDayReport,
      timeOfDayReport: TimeOfDayReport
  ): Set<DataVizReport> {
    return setOf(visitorsByDayReport, timeOfDayReport)
  }

  @Provides
  fun provideDataVizRouter(
      dataVizReportSet: Set<@JvmSuppressWildcards DataVizReport>
  ): DataVizRouter {
    return DataVizRouter(dataVizReportSet)
  }
}
