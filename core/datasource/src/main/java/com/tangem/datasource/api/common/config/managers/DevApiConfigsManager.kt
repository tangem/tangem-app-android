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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

/**
 * Implementation of [ApiConfigsManager] in DEV environment
 *
 * @param apiConfigs             api configs
 * @property appPreferencesStore app preferences store
 * @property dispatchers         coroutine dispatcher provider
 */
internal class DevApiConfigsManager(
    private val apiConfigs: ApiConfigs,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MutableApiConfigsManager() {

    override val configs: StateFlow<Map<ApiConfig, ApiEnvironment>>
        field = MutableStateFlow(value = getInitialConfigs())

    override val isInitialized: StateFlow<Boolean>
        field = MutableStateFlow(value = false)

    override fun initialize() {
        isInitialized.value = false

        appPreferencesStore.getObjectMap<ApiEnvironment>(PreferencesKeys.apiConfigsEnvironmentKey)
            .distinctUntilChanged()
            .onEach { savedEnvironments ->
                val apiConfigs = configs.value

                configs.value = apiConfigs.mapValues {
                    val (config, currentEnvironment) = it

                    savedEnvironments[config.id.name] ?: currentEnvironment
                }

                if (!isInitialized.value) {
                    isInitialized.value = true
                }

                notifyListeners(apiConfigs = apiConfigs, savedEnvironments = savedEnvironments)
            }
            .launchIn(CoroutineScope(SupervisorJob() + dispatchers.default))
    }

    override fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig {
        val apiConfigs = configs.value

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

    override suspend fun changeEnvironment(environment: ApiEnvironment) {
        val supportedConfigs = apiConfigs
            .filter { config ->
                config.environmentConfigs.any { it.environment == environment }
            }
            .map { it.id.name }

        appPreferencesStore.editData { mutablePreferences ->
            val updatedMap = mutablePreferences.getObjectMap<ApiEnvironment>(PreferencesKeys.apiConfigsEnvironmentKey)
                .mapValues { (configName, currentEnvironment) ->
                    val isEnvSupported = supportedConfigs.contains(configName)

                    if (isEnvSupported) environment else currentEnvironment
                }

            mutablePreferences.setObjectMap(key = PreferencesKeys.apiConfigsEnvironmentKey, value = updatedMap)
        }
    }

    private fun notifyListeners(
        apiConfigs: Map<ApiConfig, ApiEnvironment>,
        savedEnvironments: Map<String, ApiEnvironment>,
    ) {
        if (registerListeners.isNotEmpty()) {
            val changedConfigs = apiConfigs.mapNotNull { (config, prevEnvironment) ->
                val newEnvironment = savedEnvironments[config.id.name] ?: config.defaultEnvironment

                if (prevEnvironment == newEnvironment) return@mapNotNull null

                val environmentConfig = config.environmentConfigs
                    .firstOrNull { it.environment == newEnvironment }
                    ?: return@mapNotNull null

                config.id to environmentConfig
            }

            if (changedConfigs.isNotEmpty()) {
                registerListeners.forEach { listener ->
                    val changedConfig = changedConfigs.firstOrNull { it.first == listener.id }?.second

                    if (changedConfig != null) {
                        listener.onChange(changedConfig)
                    }
                }
            }
        }
    }

    private fun getInitialConfigs(): Map<ApiConfig, ApiEnvironment> {
        return apiConfigs.associateWith(ApiConfig::defaultEnvironment)
    }
}