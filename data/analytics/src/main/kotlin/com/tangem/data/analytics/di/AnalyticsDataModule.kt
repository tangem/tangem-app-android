package com.tangem.data.analytics.di

import com.tangem.core.analytics.repository.AnalyticsRepository
import com.tangem.data.analytics.DefaultAnalyticsRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsDataModule {

    @Provides
    fun provideAnalyticsRepository(appPreferencesStore: AppPreferencesStore): AnalyticsRepository {
        return DefaultAnalyticsRepository(appPreferencesStore)
    }
}
