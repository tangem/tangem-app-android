package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.quote.DefaultQuotesStore
import com.tangem.datasource.local.quote.QuotesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object QuotesStoreModule {

    @Provides
    @Singleton
    fun provideQuotesStore(): QuotesStore {
        return DefaultQuotesStore(
            dataStore = RuntimeDataStore(),
        )
    }
}