package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.thymeleaf.server.common.DMSHttpClient
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory

@Module
class ServerModule {

  @Provides fun provideHttpClient(loggerFactory: LoggerFactory) = DMSHttpClient(loggerFactory)
}
