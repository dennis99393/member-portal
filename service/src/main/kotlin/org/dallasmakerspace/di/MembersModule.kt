package org.dallasmakerspace.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import org.dallasmakerspace.members.observers.IMemberPropChangeObserver
import org.dallasmakerspace.members.observers.MemberDisabledObserver

@Module
abstract class MembersModule {

  @Binds
  @IntoSet
  abstract fun bindMemberObservers(observer: MemberDisabledObserver): IMemberPropChangeObserver
}
