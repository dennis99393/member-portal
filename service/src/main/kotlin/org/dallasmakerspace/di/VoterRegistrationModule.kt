package org.dallasmakerspace.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import org.dallasmakerspace.members.observers.IMemberPropChangeObserver
import org.dallasmakerspace.members.observers.VotingRegistrationStatusObserver

@Module
abstract class VoterRegistrationModule {

  @Binds
  @IntoSet
  abstract fun bindVoterRegistrationObservers(
      observer: VotingRegistrationStatusObserver
  ): IMemberPropChangeObserver
}
