package com.tangem.features.markets.di

import com.tangem.features.markets.component.MarketsListComponent
import com.tangem.features.markets.component.impl.DefaultMarketsListComponent
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
    fun bindMarketsListComponent(factory: DefaultMarketsListComponent.Factory): MarketsListComponent.Factory
}