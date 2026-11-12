package com.tangem.store.features.cardsettings.impl.di

import com.tangem.store.features.cardsettings.api.CardSettingsComponent
import com.tangem.store.features.cardsettings.impl.DefaultCardSettingsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSettingsFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultCardSettingsComponent.Factory): CardSettingsComponent.Factory
}