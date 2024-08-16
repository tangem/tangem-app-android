package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs
import com.tangem.datasource.api.common.config.ApiEnvironmentConfig

/**
 * Implementation of [ApiConfigsManager] in PROD environment
 *
 * @property apiConfigs api configs
 */
internal class ProdApiConfigsManager(
    private val apiConfigs: ApiConfigs,
) : ApiConfigsManager {

    override fun getEnvironmentConfig(id: ApiConfig.ID): ApiEnvironmentConfig {
        val config = apiConfigs.firstOrNull { it.id == id }
            ?: error("Api config with id [$id] not found. Check that ApiConfig with id [$id] was provided into DI")

        return config.environmentConfigs.firstOrNull { it.environment == config.defaultEnvironment }
            ?: error(
                "Api config with id [$id] doesn't contain environment [${config.defaultEnvironment}]. " +
                    "Check ApiConfig's environments is included default environment",
            )
    }
}