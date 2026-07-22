package com.tangem.datasource.api.common.config.managers

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiConfigs
import com.tangem.core.remote.config.ApiEnvironmentConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of [ApiConfigsManager] in PROD environment
 *
 * @property apiConfigs api configs
 */
internal class ProdApiConfigsManager(
    private val apiConfigs: ApiConfigs,
) : ApiConfigsManager {

    override val initializedState: StateFlow<Boolean> = MutableStateFlow(value = true)

    override fun initialize() = Unit

    override fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig {
        val config = apiConfigs[id.name]
            ?: error("Api config with id [${id.name}] not found. Ensure it is provided into the DI graph.")

        return config.environmentConfigs.firstOrNull { it.environment == config.defaultEnvironment }
            ?: error(
                "Api config with id [${id.name}] doesn't contain its default environment " +
                    "[${config.defaultEnvironment}]. Ensure the ApiConfig's environments include it.",
            )
    }
}