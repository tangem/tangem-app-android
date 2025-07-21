package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

/**
 * Implementation of [ApiConfigsManager] in MOCK environment
 *
 * @property apiConfigs api configs
 *
[REDACTED_AUTHOR]
 */
internal class MockApiConfigsManager(
    private val apiConfigs: ApiConfigs,
    dispatchers: CoroutineDispatcherProvider,
) : MutableApiConfigsManager() {

    override val configs: StateFlow<Map<ApiConfig, ApiEnvironment>>
    field = MutableStateFlow(value = getInitialConfigs())

    override val isInitialized: StateFlow<Boolean> = MutableStateFlow(value = true)

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override fun initialize() = Unit

    override fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig {
        val apiConfigs = configs.value

        val (config, currentEnvironment) = apiConfigs.entries.firstOrNull { it.key.id == id }
            ?: error("Api config with id [$id] not found")

        return config.environmentConfigs.firstOrNull { it.environment == currentEnvironment }
            ?: error("Api config with id [$id] doesn't contain environment [$currentEnvironment]")
    }

    override suspend fun changeEnvironment(id: String, environment: ApiEnvironment) {
        configs.update { apiConfigs ->
            val apiConfig = apiConfigs.keys.firstOrNull { it.id.name == id }
                ?: error("Api config with id [$id] not found. Check that ApiConfig with id [$id] was provided into DI")

            apiConfigs + (apiConfig to environment)
        }
    }

    override suspend fun changeEnvironment(environment: ApiEnvironment) {
        configs.update { apiConfigs ->
            apiConfigs.mapValues { (config, currentEnvironment) ->
                val isEnvSupported = config.environmentConfigs.any { it.environment == environment }

                if (isEnvSupported) environment else currentEnvironment
            }
        }
    }

    override fun addListener(listener: ApiConfigEnvChangeListener) {
        super.addListener(listener)

        configs
            .map { it.entries.firstOrNull { it.key.id == listener.id } }
            .filterNotNull()
            .onEach { (apiConfig, currentEnvironment) ->
                listener.onChange(
                    environmentConfig = apiConfig.environmentConfigs.first { it.environment == currentEnvironment },
                )
            }
            .launchIn(coroutineScope)
    }

    private fun getInitialConfigs(): Map<ApiConfig, ApiEnvironment> {
        return apiConfigs.associateWith { it.defaultEnvironment }
    }
}