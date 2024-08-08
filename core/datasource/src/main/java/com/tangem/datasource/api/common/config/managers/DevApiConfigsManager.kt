package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Implementation of [ApiConfigsManager] in DEV environment
 *
 * @property appPreferencesStore app preferences store
 * @property dispatchers         coroutine dispatcher provider
 */
internal class DevApiConfigsManager(
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MutableApiConfigsManager {

    override val configs: Flow<List<ApiConfig>> get() = _apiConfigs

    private val _apiConfigs = MutableStateFlow(value = ApiConfig.values())

    override suspend fun initialize() {
        // We can't use appPreferencesStore.getObjectMap as base flow,
        // because we should keep possibility to work with configs synchronous.
        // See [getBaseUrl]
        appPreferencesStore.getObjectMap<ApiEnvironment>(PreferencesKeys.apiConfigsEnvironmentKey)
            .onEach { savedEnvironments ->
                _apiConfigs.value = _apiConfigs.value.map { config ->
                    val savedEnvironment = savedEnvironments[config.id.name]

                    if (savedEnvironment != null) {
                        config.copySealed(currentEnvironment = savedEnvironment)
                    } else {
                        config
                    }
                }
            }
            .launchIn(CoroutineScope(dispatchers.main))
    }

    override fun getBaseUrl(id: ApiConfig.ID): String {
        val config = _apiConfigs.value.firstOrNull { it.id == id }
            ?: error("Api config with id [$id] not found. Check ApiConfig implementations")

        return config.environments[config.currentEnvironment]
            ?: error(
                "Api config with id [$id] doesn't contain environment [${config.currentEnvironment}]. " +
                    "Check ApiConfig implementations",
            )
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
