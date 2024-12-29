package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.activedirectory.ActiveDirectoryClient
import org.dallasmakerspace.activedirectory.ActiveDirectoryClientMock
import org.dallasmakerspace.activedirectory.ActiveDirectoryService
import org.dallasmakerspace.activedirectory.ActiveDirectoryServiceMock
import org.dallasmakerspace.activedirectory.IActiveDirectoryClient
import org.dallasmakerspace.activedirectory.IActiveDirectoryService
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory

@Module
class ActiveDirectoryModule {

  @Provides
  fun providesActiveDirectoryClient(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): IActiveDirectoryClient =
      if (useMockServices(appConfig)) {
        ActiveDirectoryClientMock()
        // ActiveDirectoryClient(appConfig)
      } else {
        ActiveDirectoryClient(appConfig, loggerFactory)
      }

  private fun useMockServices(appConfig: AppConfig) =
      appConfig.requireBooleanProperty("ktor.development") &&
          appConfig.requireBooleanProperty("app.use-mock-services")

  @Provides
  fun provideActiveDirectoryService(
      appConfig: AppConfig,
      activeDirectoryClient: IActiveDirectoryClient
  ): IActiveDirectoryService =
      if (appConfig.requireBooleanProperty("ktor.development")) {
        ActiveDirectoryServiceMock()
        // ActiveDirectoryService(appConfig, activeDirectoryClient)
      } else {
        ActiveDirectoryService(activeDirectoryClient)
      }
}
