package com.tangem.data.settings.di

import android.content.Context
import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultPermissionRepository
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        appPreferencesStore: AppPreferencesStore,
        appLogsStore: AppLogsStore,
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): SettingsRepository {
        return DefaultSettingsRepository(
            appPreferencesStore = appPreferencesStore,
            appLogsStore = appLogsStore,
            tangemTechApi = tangemTechApi,
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
    fun providePushPermissionRepository(
        appPreferencesStore: AppPreferencesStore,
        @ApplicationContext context: Context,
    ): PermissionRepository {
        return DefaultPermissionRepository(
            appPreferencesStore = appPreferencesStore,
            context = context,
        )
    }
}