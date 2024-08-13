package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiConfigs

/**
 * Implementation of [ApiConfigsManager] in PROD environment
 *
 * @property apiConfigs api configs
 */
internal class ProdApiConfigsManager(
    private val apiConfigs: ApiConfigs,
) : ApiConfigsManager {

    override fun getBaseUrl(id: ApiConfig.ID): String {
        val config = apiConfigs.firstOrNull { it.id == id }
            ?: error("Api config with id [$id] not found. Check ApiConfig implementations")

        return config.environments[config.defaultEnvironment]
            ?: error(
                "Api config with id [$id] doesn't contain " +
                    "environment [${config.defaultEnvironment}]. Check ApiConfig implementations",
            )
    }
}