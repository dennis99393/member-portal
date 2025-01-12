package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.routes.IRouteHandler
import org.dallasmakerspace.server.voterregistration.RegisterVotingHandler
import org.dallasmakerspace.server.voterregistration.UnregisterVotingHandler

@Module
class VoterRegistrationModule {

  @IntoMap
  @Provides
  @StringKey("/profile/@{preferred_username}/register-voting")
  fun providesRegisterVotingHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
  ): IRouteHandler =
      RegisterVotingHandler(
          loggerFactory,
          userInfoProvider,
          memberService,
      )

  @IntoMap
  @Provides
  @StringKey("/profile/@{preferred_username}/unregister-voting")
  fun providesUnregisterVotingHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
  ): IRouteHandler =
      UnregisterVotingHandler(
          loggerFactory,
          userInfoProvider,
          memberService,
      )
}
