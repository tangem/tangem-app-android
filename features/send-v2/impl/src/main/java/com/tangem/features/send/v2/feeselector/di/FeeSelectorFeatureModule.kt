package com.tangem.features.send.v2.feeselector.di

import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadListener
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.v2.feeselector.DefaultFeeSelectorBlockComponent
import com.tangem.features.send.v2.feeselector.DefaultFeeSelectorComponent
import com.tangem.features.send.v2.feeselector.DefaultFeeSelectorReloadTrigger
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

    @Binds
    @Singleton
    fun provideFeeSelectorReloadTrigger(impl: DefaultFeeSelectorReloadTrigger): FeeSelectorReloadTrigger

    @Binds
    @Singleton
    fun provideFeeSelectorReloadListener(impl: DefaultFeeSelectorReloadTrigger): FeeSelectorReloadListener

    @Binds
    @Singleton
    fun provideFeeSelectorCheckReloadTrigger(impl: DefaultFeeSelectorReloadTrigger): FeeSelectorCheckReloadTrigger

    @Binds
    @Singleton
    fun provideFeeSelectorCheckReloadListener(impl: DefaultFeeSelectorReloadTrigger): FeeSelectorCheckReloadListener
}