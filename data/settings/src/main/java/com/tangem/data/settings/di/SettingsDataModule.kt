package com.tangem.data.settings.di

import android.content.Context
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.settings.BuildConfig
import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultPermissionRepository
import com.tangem.data.settings.DefaultSettingsFeatureToggles
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.data.settings.DevHotWalletRestrictionManager
import com.tangem.data.settings.DevUsedeskTokenTtlManager
import com.tangem.data.settings.ProdHotWalletRestrictionManager
import com.tangem.data.settings.ProdUsedeskTokenTtlManager
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.domain.settings.SettingsFeatureToggles
import com.tangem.domain.settings.UsedeskTokenTtlManager
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
        storePreferencesKeys: StorePreferencesKeys,
        appLogsStore: AppLogsStore,
        tangemTechApi: TangemTechApi,
        settingsFeatureToggles: SettingsFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): SettingsRepository {
        return DefaultSettingsRepository(
            appPreferencesStore = appPreferencesStore,
            storePreferencesKeys = storePreferencesKeys,
            appLogsStore = appLogsStore,
            tangemTechApi = tangemTechApi,
            settingsFeatureToggles = settingsFeatureToggles,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideSettingsFeatureToggles(featureTogglesManager: FeatureTogglesManager): SettingsFeatureToggles {
        return DefaultSettingsFeatureToggles(featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideAppRatingRepository(
        appPreferencesStore: AppPreferencesStore,
        storePreferencesKeys: StorePreferencesKeys,
    ): AppRatingRepository {
        return DefaultAppRatingRepository(
            appPreferencesStore = appPreferencesStore,
            storePreferencesKeys = storePreferencesKeys,
        )
    }

    @Provides
    @Singleton
    fun providePushPermissionRepository(
        appPreferencesStore: AppPreferencesStore,
        storePreferencesKeys: StorePreferencesKeys,
        @ApplicationContext context: Context,
    ): PermissionRepository {
        return DefaultPermissionRepository(
            appPreferencesStore = appPreferencesStore,
            storePreferencesKeys = storePreferencesKeys,
            context = context,
        )
    }

    @Provides
    @Singleton
    fun provideHotWalletRestrictionManager(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): HotWalletRestrictionManager {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevHotWalletRestrictionManager(appPreferencesStore, dispatchers)
        } else {
            ProdHotWalletRestrictionManager()
        }
    }

    @Provides
    @Singleton
    fun provideUsedeskTokenTtlManager(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): UsedeskTokenTtlManager {
        return if (BuildConfig.TESTER_MENU_ENABLED) {
            DevUsedeskTokenTtlManager(appPreferencesStore, dispatchers)
        } else {
            ProdUsedeskTokenTtlManager()
        }
    }
}