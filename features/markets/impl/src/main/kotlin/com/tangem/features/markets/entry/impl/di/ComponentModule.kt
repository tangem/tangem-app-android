package com.tangem.features.markets.entry.impl.di

import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.markets.entry.impl.DefaultMarketsEntryComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindMarketsEntryComponent(factory: DefaultMarketsEntryComponent.Factory): MarketsEntryComponent.Factory
}