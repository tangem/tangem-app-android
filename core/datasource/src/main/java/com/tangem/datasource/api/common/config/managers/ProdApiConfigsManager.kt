package com.tangem.datasource.api.common.config.managers

import com.tangem.datasource.api.common.config.ApiConfig

/** Implementation of [ApiConfigsManager] in PROD environment */
internal class ProdApiConfigsManager : ApiConfigsManager {

    override fun getBaseUrl(id: ApiConfig.ID): String {
        val config = ApiConfig.values().firstOrNull { it.id == id }
            ?: error("Api config with id [$id] not found. Check ApiConfig implementations")

        return config.environments[config.defaultEnvironment]
            ?: error(
                "Api config with id [$id] doesn't contain " +
                    "environment [${config.defaultEnvironment}]. Check ApiConfig implementations",
            )
    }
}
