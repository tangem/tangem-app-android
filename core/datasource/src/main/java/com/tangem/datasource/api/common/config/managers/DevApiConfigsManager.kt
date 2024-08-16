package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Implementation of [ApiConfigsManager] in DEV environment
 *
 * @param apiConfigs             api configs
 * @property appPreferencesStore app preferences store
 * @property dispatchers         coroutine dispatcher provider
 */
internal class DevApiConfigsManager(
    apiConfigs: ApiConfigs,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MutableApiConfigsManager {

    override val configs: Flow<Map<ApiConfig, ApiEnvironment>> get() = _apiConfigs

    private val _apiConfigs = MutableStateFlow(value = apiConfigs.associateWith { it.defaultEnvironment })

    override suspend fun initialize() {
        // We can't use appPreferencesStore.getObjectMap as base flow,
        // because we should keep possibility to work with configs synchronous.
        // See [getBaseUrl]
        appPreferencesStore.getObjectMap<ApiEnvironment>(PreferencesKeys.apiConfigsEnvironmentKey)
            .onEach { savedEnvironments ->
                _apiConfigs.update { apiConfigs ->
                    apiConfigs.mapValues {
                        val (config, currentEnvironment) = it

                        savedEnvironments[config.id.name] ?: currentEnvironment
                    }
                }
            }
            .launchIn(CoroutineScope(dispatchers.main))
    }

    override fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig {
        val apiConfigs = _apiConfigs.value

        val config = apiConfigs.map { it }.firstOrNull { it.key.id == id }?.key
            ?: error("Api config with id [$id] not found")

        val currentEnvironment = apiConfigs[config]
            ?: error("Current environment of api config with id [$id] not found")

        return config.environmentConfigs.firstOrNull { it.environment == currentEnvironment }
            ?: error("Api config with id [$id] doesn't contain environment [$currentEnvironment]")
    }

    override suspend fun changeEnvironment(id: String, environment: ApiEnvironment) {
        appPreferencesStore.editData { mutablePreferences ->
            val updatedMap = mutablePreferences.getObjectMap<ApiEnvironment>(PreferencesKeys.apiConfigsEnvironmentKey)
                .toMutableMap()
                .apply { put(id, environment) }

            mutablePreferences.setObjectMap(PreferencesKeys.apiConfigsEnvironmentKey, updatedMap)
        }
    }
}