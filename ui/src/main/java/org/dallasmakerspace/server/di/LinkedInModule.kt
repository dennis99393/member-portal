package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.linkedin.LinkLinkedInHandler
import org.dallasmakerspace.server.linkedin.LinkedInCallbackHandler
import org.dallasmakerspace.server.linkedin.LinkedInOAuthProvider
import org.dallasmakerspace.server.linkedin.LinkedInStateCache
import org.dallasmakerspace.server.linkedin.UnlinkLinkedInHandler
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.routes.IRouteHandler

@Module
class LinkedInModule {

  @Provides
  fun providesLinkedInOAuthProvider(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): LinkedInOAuthProvider = LinkedInOAuthProvider(appConfig, loggerFactory)

  @IntoMap
  @Provides
  @StringKey("/link-linkedin")
  fun providesLinkLinkedInHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      linkedInStateCache: LinkedInStateCache,
      linkedInOAuthProvider: LinkedInOAuthProvider
  ): IRouteHandler =
      LinkLinkedInHandler(
          loggerFactory, userInfoProvider, linkedInStateCache, linkedInOAuthProvider)

  @IntoMap
  @Provides
  @StringKey("/unlink-linkedin")
  fun providesUnlinkLinkedInHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
  ): IRouteHandler = UnlinkLinkedInHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/linkedin-callback")
  fun providesLinkedInCallbackHandler(
      loggerFactory: LoggerFactory,
      linkedInOAuthProvider: LinkedInOAuthProvider,
      memberService: MemberService,
      userInfoProvider: UserInfoProvider,
      linkedInStateCache: LinkedInStateCache
  ): IRouteHandler =
      LinkedInCallbackHandler(
          loggerFactory, linkedInOAuthProvider, memberService, userInfoProvider, linkedInStateCache)
}
