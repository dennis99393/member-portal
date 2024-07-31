package org.dallasmakerspace.thymeleaf.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.thymeleaf.server.auth.UserInfoProvider
import org.dallasmakerspace.thymeleaf.server.common.logging.LoggerFactory
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseCallbackHandler
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseNonceCache
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseSSOProvider
import org.dallasmakerspace.thymeleaf.server.discourse.DiscourseUtil
import org.dallasmakerspace.thymeleaf.server.discourse.LinkDiscourseHandler
import org.dallasmakerspace.thymeleaf.server.discourse.UnlinkDiscourseHandler
import org.dallasmakerspace.thymeleaf.server.memberservice.MemberService
import org.dallasmakerspace.thymeleaf.server.routes.IRouteHandler

@Module
class DiscourseModule {

  //  @Provides fun providesDiscourseUtil(): DiscourseUtil = DiscourseUtil()

  /*  @Provides
  fun providesDiscourseSSOProvider(discourseUtil: DiscourseUtil): DiscourseSSOProvider =
      DiscourseSSOProvider(discourseUtil)*/

  @IntoMap
  @Provides
  @StringKey("/link-discourse")
  fun providesLinkDiscourseHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      discourseNonceCache: DiscourseNonceCache,
      discourseSSOProvider: DiscourseSSOProvider
  ): IRouteHandler =
      LinkDiscourseHandler(
          loggerFactory,
          userInfoProvider,
          discourseNonceCache,
          discourseSSOProvider,
      )

  @IntoMap
  @Provides
  @StringKey("/unlink-discourse")
  fun providesUnlinkDiscourseHandler(
      loggerFactory: LoggerFactory,
      memberService: MemberService,
      userInfoProvider: UserInfoProvider
  ): IRouteHandler = UnlinkDiscourseHandler(loggerFactory, memberService, userInfoProvider)

  @IntoMap
  @Provides
  @StringKey("/discourse-callback")
  fun providesDiscourseHandler(
      loggerFactory: LoggerFactory,
      discourseUtil: DiscourseUtil,
      memberService: MemberService,
      userInfoProvider: UserInfoProvider,
      discourseNonceCache: DiscourseNonceCache
  ): IRouteHandler =
      DiscourseCallbackHandler(
          loggerFactory, discourseUtil, memberService, userInfoProvider, discourseNonceCache)
}
