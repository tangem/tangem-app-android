package com.tangem.data.settings.di

import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.data.settings.DefaultPromoSettingsRepository
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.repositories.PromoSettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        preferencesDataSource: PreferencesDataSource,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): SettingsRepository {
        return DefaultSettingsRepository(
            preferencesDataSource = preferencesDataSource,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideAppRatingRepository(appPreferencesStore: AppPreferencesStore): AppRatingRepository {
        return DefaultAppRatingRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun providePromoSettingsSettingsRepository(appPreferencesStore: AppPreferencesStore): PromoSettingsRepository {
        return DefaultPromoSettingsRepository(appPreferencesStore = appPreferencesStore)
    }
}
