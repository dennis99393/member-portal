package org.dallasmakerspace.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import org.dallasmakerspace.members.observers.DiscourseMemberStatusObserver
import org.dallasmakerspace.members.observers.IMemberPropChangeObserver

@Module
abstract class MembersModule {

  @Binds
  @IntoSet
  abstract fun bindDiscourseMemberObservers(
      observer: DiscourseMemberStatusObserver
  ): IMemberPropChangeObserver
}
