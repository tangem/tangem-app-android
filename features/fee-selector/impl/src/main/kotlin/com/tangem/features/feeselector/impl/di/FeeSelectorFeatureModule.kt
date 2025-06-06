package com.tangem.features.feeselector.impl.di

import com.tangem.features.feeselector.api.component.FeeSelectorComponent
import com.tangem.features.feeselector.impl.component.DefaultFeeSelectorComponent
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
}