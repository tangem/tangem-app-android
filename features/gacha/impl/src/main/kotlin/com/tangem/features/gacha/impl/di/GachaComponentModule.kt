package com.tangem.features.gacha.impl.di

import com.tangem.features.gacha.api.GachaEntryComponent
import com.tangem.features.gacha.impl.DefaultGachaEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GachaComponentModule {

    @Binds
    @Singleton
    fun bindGachaEntryComponentFactory(impl: DefaultGachaEntryComponent.Factory): GachaEntryComponent.Factory
}