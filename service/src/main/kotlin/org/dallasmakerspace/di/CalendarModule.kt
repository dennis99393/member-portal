package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.dallasmakerspace.calendar.CalendarRepository
import org.dallasmakerspace.calendar.CalendarService
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.db.master.GenericRepository

@Module
class CalendarModule {

  @Provides
  @Singleton
  fun provideCalendarRepository(
      genericRepository: GenericRepository,
      loggerFactory: LoggerFactory
  ): CalendarRepository {
    return CalendarRepository(genericRepository, loggerFactory)
  }

  @Provides
  @Singleton
  fun provideCalendarService(
      calendarRepository: CalendarRepository,
      loggerFactory: LoggerFactory
  ): CalendarService {
    return CalendarService(calendarRepository, loggerFactory)
  }
}
