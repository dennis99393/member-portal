package org.dallasmakerspace.dataviz.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.ElementsIntoSet
import org.dallasmakerspace.dataviz.DataVizReport
import org.dallasmakerspace.dataviz.DataVizRouter
import org.dallasmakerspace.dataviz.reports.VisitorsByDayReport
import org.dallasmakerspace.db.master.GenericRepository

@Module
class DataVizModule {

  @Provides
  fun provideVisitorsByDayReport(genericRepository: GenericRepository): VisitorsByDayReport {
    return VisitorsByDayReport(genericRepository)
  }

  @Provides
  @ElementsIntoSet
  fun provideVisitorsByDayReports(visitorsByDayReport: VisitorsByDayReport): Set<DataVizReport> {
    return setOf(visitorsByDayReport)
  }

  @Provides
  fun provideDataVizRouter(
      dataVizReportSet: Set<@JvmSuppressWildcards DataVizReport>
  ): DataVizRouter {
    return DataVizRouter(dataVizReportSet)
  }
}
