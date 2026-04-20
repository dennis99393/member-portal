package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.guacamole.GuacamoleApiClient
import org.dallasmakerspace.guacamole.GuacamoleApiClientMock
import org.dallasmakerspace.guacamole.IGuacamoleApiClient

@Module
class GuacamoleModule {

  @Provides
  fun provideGuacamoleApiClient(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): IGuacamoleApiClient =
      if (useMockServices(appConfig)) {
        GuacamoleApiClientMock(loggerFactory)
      } else {
        GuacamoleApiClient(appConfig, loggerFactory)
      }

  private fun useMockServices(appConfig: AppConfig) =
      appConfig.requireBooleanProperty("ktor.development") &&
          appConfig.requireBooleanProperty("app.use-mock-services")
}
