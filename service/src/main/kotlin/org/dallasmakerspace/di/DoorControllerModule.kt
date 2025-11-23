package org.dallasmakerspace.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.core.LoggerFactory
import org.dallasmakerspace.cron.DoorSwipesCronJob
import org.dallasmakerspace.doorcontroller.DoorControllerConfig
import org.dallasmakerspace.doorcontroller.DoorControllerService
import javax.inject.Singleton

@Module
class DoorControllerModule {

    @Provides
    @Singleton
    fun provideDoorControllerConfig(appConfig: AppConfig): DoorControllerConfig {
        return DoorControllerConfig.fromAppConfig(appConfig)
    }

    @Provides
    @Singleton
    fun provideDoorControllerService(
        config: DoorControllerConfig,
        loggerFactory: LoggerFactory
    ): DoorControllerService {
        return DoorControllerService(config, loggerFactory)
    }

    @Provides
    @Reusable
    fun provideDoorSwipesCronJob(
        doorControllerService: DoorControllerService,
        loggerFactory: LoggerFactory
    ): DoorSwipesCronJob {
        return DoorSwipesCronJob(loggerFactory, doorControllerService)
    }
}
