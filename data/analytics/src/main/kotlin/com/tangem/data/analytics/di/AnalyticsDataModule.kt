package com.tangem.data.analytics.di

import com.tangem.data.analytics.DefaultAnalyticsRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.domain.analytics.repository.AnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsDataModule {

    @Provides
    fun provideAnalyticsRepository(
        appPreferencesStore: AppPreferencesStore,
        storePreferencesKeys: StorePreferencesKeys,
    ): AnalyticsRepository {
        return DefaultAnalyticsRepository(appPreferencesStore, storePreferencesKeys)
    }
}