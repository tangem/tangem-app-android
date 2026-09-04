package com.tangem.datasource.remote.config.managers

import com.tangem.datasource.BuildConfig
import com.tangem.core.remote.config.ApiConfig.Companion.MOCKED_BUILD_TYPE
import com.tangem.core.remote.config.ApiConfigs
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ApiConfigsManagerModule {

    @Provides
    @Singleton
    fun provideApiConfigManager(
        apiConfigs: ApiConfigs,
        appPreferencesStore: AppPreferencesStore,
        appScope: AppCoroutineScope,
    ): ApiConfigsManager {
        // The DI key (the @StringKey where each config is registered) and the config's own id are
        // declared separately; fail fast on startup if they drift, otherwise the config would be
        // silently unreachable by id.
        apiConfigs.forEach { (key, config) ->
            check(key == config.id.name) {
                "ApiConfig DI key [$key] doesn't match the config id [${config.id.name}]. " +
                    "The @StringKey must match the config's own id."
            }
        }

        return when {
            BuildConfig.BUILD_TYPE == MOCKED_BUILD_TYPE -> MockApiConfigsManager(apiConfigs, appScope)
            BuildConfig.TESTER_MENU_ENABLED -> DevApiConfigsManager(apiConfigs, appPreferencesStore, appScope)
            else -> ProdApiConfigsManager(apiConfigs)
        }
    }
}