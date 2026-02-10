package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.memberservice.MemberServiceClient
import org.dallasmakerspace.server.routes.ActionTrackingHandler
import org.dallasmakerspace.server.routes.AskAiHandler
import org.dallasmakerspace.server.routes.BackendApiHandler
import org.dallasmakerspace.server.routes.CommitteeDetailHandler
import org.dallasmakerspace.server.routes.CommitteesHandler
import org.dallasmakerspace.server.routes.GroupsHandler
import org.dallasmakerspace.server.routes.IRouteHandler
import org.dallasmakerspace.server.routes.IndexHandler
import org.dallasmakerspace.server.routes.LoginHandler
import org.dallasmakerspace.server.routes.ManifestHandler
import org.dallasmakerspace.server.routes.OfflineHandler
import org.dallasmakerspace.server.routes.OidcCallbackHandler
import org.dallasmakerspace.server.routes.PingHandler
import org.dallasmakerspace.server.routes.ProfileHandler
import org.dallasmakerspace.server.routes.ProfileMeHandler
import org.dallasmakerspace.server.routes.ReportHandler
import org.dallasmakerspace.server.routes.SearchPreloadHandler
import org.dallasmakerspace.server.routes.ShortLinkDetailsHandler
import org.dallasmakerspace.server.routes.ShortLinkRedirectHandler
import org.dallasmakerspace.server.routes.ShortLinksAdminHandler
import org.dallasmakerspace.server.routes.ShortLinksHandler
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
      ProfileHandler(loggerFactory, userInfoProvider, memberService, voterRegistrationManager)

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
  ): IRouteHandler = GroupsHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/reports/{path...}")
  fun providesReportHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
  ): IRouteHandler = ReportHandler(loggerFactory, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/backend-api/{path...}")
  fun providesBackendApiHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = BackendApiHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/short-links")
  fun providesShortLinksHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
      appConfig: AppConfig
  ): IRouteHandler = ShortLinksHandler(loggerFactory, userInfoProvider, memberService, appConfig)

  @IntoMap
  @Provides
  @StringKey("/short-links/admin")
  fun providesShortLinksAdminHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
      appConfig: AppConfig
  ): IRouteHandler =
      ShortLinksAdminHandler(loggerFactory, userInfoProvider, memberService, appConfig)

  @IntoMap
  @Provides
  @StringKey("/short-links/{id}")
  fun providesShortLinkDetailsHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = ShortLinkDetailsHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/go/{path...}")
  fun providesShortLinkRedirectHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberServiceClient: MemberServiceClient
  ): IRouteHandler = ShortLinkRedirectHandler(loggerFactory, userInfoProvider, memberServiceClient)

  @IntoMap
  @Provides
  @StringKey("/committees")
  fun providesCommitteesHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = CommitteesHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/committees/{committee_slug}")
  fun providesCommitteeDetailHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService
  ): IRouteHandler = CommitteeDetailHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/api/track")
  fun providesActionTrackingHandler(
      loggerFactory: LoggerFactory,
  ): IRouteHandler = ActionTrackingHandler(loggerFactory)

  @IntoMap
  @Provides
  @StringKey("/ask-ai")
  fun providesAskAiHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
      appConfig: AppConfig
  ): IRouteHandler = AskAiHandler(loggerFactory, userInfoProvider, memberService, appConfig)

  @IntoMap
  @Provides
  @StringKey("/manifest.json")
  fun providesManifestHandler(loggerFactory: LoggerFactory, appConfig: AppConfig): IRouteHandler =
      ManifestHandler(loggerFactory, appConfig)

  @IntoMap
  @Provides
  @StringKey("/offline")
  fun providesOfflineHandler(loggerFactory: LoggerFactory): IRouteHandler =
      OfflineHandler(loggerFactory)
}
