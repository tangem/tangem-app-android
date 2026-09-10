package com.tangem.datasource.remote.config.managers

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

/** Builds a minimal [ApiConfig] with the given id, default environment and supported environments. */
internal fun apiConfig(
    id: String,
    default: ApiEnvironment,
    environments: List<ApiEnvironment> = listOf(default),
): ApiConfig = object : ApiConfig() {
    override val id: ApiConfig.ID = ApiConfig.ID(id)
    override val defaultEnvironment: ApiEnvironment = default
    override val environmentConfigs: List<ApiEnvironmentConfig> = environments.map { environment ->
        ApiEnvironmentConfig(environment = environment, baseUrl = "https://$id.tests.com/")
    }
}