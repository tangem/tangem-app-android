package com.tangem.data.settings.di

import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultPermissionRepository
import com.tangem.data.settings.DefaultPromoSettingsRepository
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.settings.repositories.PromoSettingsRepository
import com.tangem.domain.settings.repositories.SettingsRepository
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
    fun providePromoSettingsSettingsRepository(appPreferencesStore: AppPreferencesStore): PromoSettingsRepository {
        return DefaultPromoSettingsRepository(appPreferencesStore = appPreferencesStore)
    }

    @Provides
    @Singleton
    fun providePushPermissionRepository(appPreferencesStore: AppPreferencesStore): PermissionRepository {
        return DefaultPermissionRepository(appPreferencesStore = appPreferencesStore)
    }
}