package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.routes.BackendApiHandler
import org.dallasmakerspace.server.routes.GroupsHandler
import org.dallasmakerspace.server.routes.IRouteHandler
import org.dallasmakerspace.server.routes.IndexHandler
import org.dallasmakerspace.server.routes.LoginHandler
import org.dallasmakerspace.server.routes.OidcCallbackHandler
import org.dallasmakerspace.server.routes.PingHandler
import org.dallasmakerspace.server.routes.ProfileHandler
import org.dallasmakerspace.server.routes.ProfileMeHandler
import org.dallasmakerspace.server.routes.ReportHandler
import org.dallasmakerspace.server.routes.SearchPreloadHandler
import org.dallasmakerspace.server.voterregistration.VoterRegistrationManager

@Module
class RoutesModule {

  @IntoMap
  @Provides
  @StringKey("/ping")
  fun providesPingHandler(
      loggerFactory: LoggerFactory,
      memberService: MemberService
  ): IRouteHandler = PingHandler(loggerFactory, memberService)

  @IntoMap
  @Provides
  @StringKey("/search-preload")
  fun providesSearchPreloadHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
  ): IRouteHandler = SearchPreloadHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/")
  fun providesIndexHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider
  ): IRouteHandler = IndexHandler(loggerFactory, userInfoProvider)

  @IntoMap @Provides @StringKey("/login") fun providesLoginHandler(): IRouteHandler = LoginHandler()

  @IntoMap
  @Provides
  @StringKey("/profile/@{preferred_username}")
  fun providesProfileHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
      voterRegistrationManager: VoterRegistrationManager
  ): IRouteHandler =
      ProfileHandler(loggerFactory, memberService, userInfoProvider, voterRegistrationManager)

  @IntoMap
  @Provides
  @StringKey("/profile-me")
  fun providesProfileMeHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
  ): IRouteHandler = ProfileMeHandler(loggerFactory, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/oidc-callback")
  fun providesOidcCallbackHandler(): IRouteHandler = OidcCallbackHandler()

  @IntoMap
  @Provides
  @StringKey("/groups/{group_slug}")
  fun providesGroupHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = GroupsHandler(loggerFactory, memberService, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/reports/{...}")
  fun providesReportHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
  ): IRouteHandler = ReportHandler(loggerFactory, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/backend-api/{...}")
  fun providesBackendApiHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = BackendApiHandler(loggerFactory, memberService, userInfoProvider)
}
