package org.dallasmakerspace.config.di

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.dallasmakerspace.config.ConfigOverrideService
import org.dallasmakerspace.config.db.ConfigOverrideRepository
import org.dallasmakerspace.core.LoggerFactory

@Module
class ConfigModule {
  @Provides
  @Singleton
  fun provideConfigOverrideRepository(loggerFactory: LoggerFactory): ConfigOverrideRepository =
      ConfigOverrideRepository(loggerFactory)

  @Provides
  @Singleton
  fun provideConfigOverrideService(
      repository: ConfigOverrideRepository,
      loggerFactory: LoggerFactory,
  ): ConfigOverrideService = ConfigOverrideService(repository, loggerFactory)
}
