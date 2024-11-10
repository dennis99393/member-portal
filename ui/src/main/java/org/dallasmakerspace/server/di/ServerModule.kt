package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.server.common.DMSHttpClient
import org.dallasmakerspace.server.common.logging.LoggerFactory

@Module
class ServerModule {

  @Provides fun provideHttpClient(loggerFactory: LoggerFactory) = DMSHttpClient(loggerFactory)
}
