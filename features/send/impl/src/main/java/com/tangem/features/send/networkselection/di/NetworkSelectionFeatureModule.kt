package com.tangem.features.send.networkselection.di

import com.tangem.features.send.api.NetworkSelectionComponent
import com.tangem.features.send.networkselection.DefaultNetworkSelectionComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkSelectionFeatureModule {

    @Binds
    @Singleton
    fun bindNetworkSelectionComponentFactory(
        impl: DefaultNetworkSelectionComponent.Factory,
    ): NetworkSelectionComponent.Factory
}