package com.tangem.data.appsflyer.store

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
    fun provideAppFlyerStore(appPreferencesStore: AppPreferencesStore): AppsFlyerStore {
        return DefaultAppsFlyerStore(appPreferencesStore = appPreferencesStore)
    }
}