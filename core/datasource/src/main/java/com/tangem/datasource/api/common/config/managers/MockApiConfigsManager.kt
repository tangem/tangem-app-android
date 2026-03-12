package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.*
import kotlin.Boolean
import kotlin.String
import kotlin.Unit
import kotlin.collections.Map
import kotlin.collections.any
import kotlin.collections.associateWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.mapValues
import kotlin.collections.plus
import kotlin.error
import kotlin.to

/**
 * Implementation of [ApiConfigsManager] in MOCK environment
 *
 * @property apiConfigs api configs
 *
[REDACTED_AUTHOR]
 */
internal class MockApiConfigsManager(
    private val apiConfigs: ApiConfigs,
    private val coroutineScope: AppCoroutineScope,
) : MutableApiConfigsManager() {

    override val configs: StateFlow<Map<ApiConfig, ApiEnvironment>>
        field = MutableStateFlow(value = getInitialConfigs())

    override val initializedState: StateFlow<Boolean> = MutableStateFlow(value = true)

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
            .map { map -> map.entries.firstOrNull { it.key.id == listener.id } }
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