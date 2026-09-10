package com.tangem.store.datasource.di

import com.tangem.datasource.local.markets.CoinCategoriesSnapshotStore
import com.tangem.datasource.local.markets.DefaultCoinCategoriesSnapshotStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CoinCategoriesDataModule {

    @Provides
    @Singleton
    fun provideCoinCategoriesSnapshotStore(appPreferencesStore: AppPreferencesStore): CoinCategoriesSnapshotStore {
        return DefaultCoinCategoriesSnapshotStore(appPreferencesStore)
    }
}