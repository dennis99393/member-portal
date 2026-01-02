package org.dallasmakerspace.server.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.dallasmakerspace.server.auth.UserInfoProvider
import org.dallasmakerspace.server.common.AppConfig
import org.dallasmakerspace.server.common.logging.LoggerFactory
import org.dallasmakerspace.server.discord.DiscordCallbackHandler
import org.dallasmakerspace.server.discord.DiscordOAuthProvider
import org.dallasmakerspace.server.discord.DiscordStateCache
import org.dallasmakerspace.server.discord.LinkDiscordHandler
import org.dallasmakerspace.server.discord.UnlinkDiscordHandler
import org.dallasmakerspace.server.memberservice.MemberService
import org.dallasmakerspace.server.routes.IRouteHandler

@Module
class DiscordModule {

  @Provides
  fun providesDiscordOAuthProvider(
      appConfig: AppConfig,
      loggerFactory: LoggerFactory
  ): DiscordOAuthProvider = DiscordOAuthProvider(appConfig, loggerFactory)

  @IntoMap
  @Provides
  @StringKey("/link-discord")
  fun providesLinkDiscordHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      discordStateCache: DiscordStateCache,
      discordOAuthProvider: DiscordOAuthProvider
  ): IRouteHandler =
      LinkDiscordHandler(loggerFactory, userInfoProvider, discordStateCache, discordOAuthProvider)

  @IntoMap
  @Provides
  @StringKey("/unlink-discord")
  fun providesUnlinkDiscordHandler(
      loggerFactory: LoggerFactory,
      userInfoProvider: UserInfoProvider,
      memberService: MemberService,
  ): IRouteHandler = UnlinkDiscordHandler(loggerFactory, userInfoProvider, memberService)

  @IntoMap
  @Provides
  @StringKey("/discord-callback")
  fun providesDiscordCallbackHandler(
      loggerFactory: LoggerFactory,
      discordOAuthProvider: DiscordOAuthProvider,
      memberService: MemberService,
      userInfoProvider: UserInfoProvider,
      discordStateCache: DiscordStateCache
  ): IRouteHandler =
      DiscordCallbackHandler(
          loggerFactory, discordOAuthProvider, memberService, userInfoProvider, discordStateCache)
}
