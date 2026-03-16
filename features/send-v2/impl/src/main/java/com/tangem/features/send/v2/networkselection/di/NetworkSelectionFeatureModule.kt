package com.tangem.features.send.v2.networkselection.di

import com.tangem.features.send.v2.api.NetworkSelectionComponent
import com.tangem.features.send.v2.networkselection.DefaultNetworkSelectionComponent
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