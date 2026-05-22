package com.tangem.datasource.api.common.config

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.utils.ProviderSuspend

internal class SurveySparrow(
    private val environmentConfig: EnvironmentConfig,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://eu-api.surveysparrow.com/",
        headers = buildMap {
            put(
                key = "Authorization",
                value = ProviderSuspend { "Bearer ${environmentConfig.surveySparrowToken.orEmpty()}" },
            )
        },
    )
}