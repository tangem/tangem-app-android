package com.tangem.features.send.v2.feeselector.di

import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.feeselector.DefaultFeeSelectorBlockComponent
import com.tangem.features.send.v2.feeselector.DefaultFeeSelectorComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FeeSelectorFeatureModule {

    @Binds
    @Singleton
    fun bindComponentFactory(factory: DefaultFeeSelectorComponent.Factory): FeeSelectorComponent.Factory

    @Binds
    @Singleton
    fun bindBlockComponentFactory(factory: DefaultFeeSelectorBlockComponent.Factory): FeeSelectorBlockComponent.Factory
}