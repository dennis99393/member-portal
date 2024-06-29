package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import org.dallasmakerspace.thymeleaf.server.common.DMSHttpClient

@Module
class ServerModule {

  @Provides fun provideHttpClient() = DMSHttpClient()
}
