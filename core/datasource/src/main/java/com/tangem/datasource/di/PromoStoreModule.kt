package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.promo.DefaultPromoStoriesStore
import com.tangem.datasource.local.promo.PromoStoriesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PromoStoreModule {

    @Provides
    @Singleton
    fun providePromoStoriesStore(): PromoStoriesStore {
        return DefaultPromoStoriesStore(dataStore = RuntimeDataStore())
    }
}