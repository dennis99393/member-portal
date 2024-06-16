package org.dallasmakerspace.di

import dagger.Component
import io.ktor.server.application.*
import javax.inject.Singleton
import org.dallasmakerspace.core.AppConfig
import org.dallasmakerspace.members.MemberService

@Singleton
@Component(modules = [AppModule::class, ActiveDirectoryModule::class, DiscourseModule::class])
interface AppComponent {
  fun inject(application: Application)

  fun getAppConfig(): AppConfig

  fun getMemberService(): MemberService
}
