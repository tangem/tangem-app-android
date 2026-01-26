package com.tangem.datasource.di

import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.datasource.local.appsflyer.DefaultAppsFlyerConversionStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppsFlyerConversionStoreModule {

    @Provides
    @Singleton
    fun provideAppFlyerConversionStore(appPreferencesStore: AppPreferencesStore): AppsFlyerConversionStore {
        return DefaultAppsFlyerConversionStore(appPreferencesStore = appPreferencesStore)
    }
}