package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultUserMarketCoinsStore
import com.tangem.datasource.local.token.UserMarketCoinsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MarketCoinsStoreModule {

    @Provides
    @Singleton
    fun provideUserMarketCoinsStore(): UserMarketCoinsStore {
        return DefaultUserMarketCoinsStore(dataStore = RuntimeDataStore())
    }
}