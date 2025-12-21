package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.shortlinks.ShortLinksRepository
import org.dallasmakerspace.shortlinks.ShortLinksService

@Module
class ShortLinksModule {

    @Provides
    @Singleton
    fun providesShortLinksRepository(loggerFactory: LoggerFactory): ShortLinksRepository {
        return ShortLinksRepository(loggerFactory)
    }

    @Provides
    @Singleton
    fun providesShortLinksService(
        loggerFactory: LoggerFactory,
        repository: ShortLinksRepository
    ): ShortLinksService {
        return ShortLinksService(loggerFactory, repository)
    }
}
