package com.tangem.features.markets.di

import com.tangem.features.markets.component.MarketsEntryComponent
import com.tangem.features.markets.DefaultMarketsEntryComponent
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
    fun bindMarketsListComponent(factory: DefaultMarketsEntryComponent.Factory): MarketsEntryComponent.Factory
}
