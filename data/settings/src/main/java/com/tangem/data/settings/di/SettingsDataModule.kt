package com.tangem.data.settings.di

import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.data.settings.DefaultSwapPromoRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.repositories.SwapPromoRepository
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
    fun provideSettingsRepository(appPreferencesStore: AppPreferencesStore): SettingsRepository {
        return DefaultSettingsRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun provideAppRatingRepository(appPreferencesStore: AppPreferencesStore): AppRatingRepository {
        return DefaultAppRatingRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun provideSwapPromoRepository(appPreferencesStore: AppPreferencesStore): SwapPromoRepository {
        return DefaultSwapPromoRepository(appPreferencesStore = appPreferencesStore)
    }
}
