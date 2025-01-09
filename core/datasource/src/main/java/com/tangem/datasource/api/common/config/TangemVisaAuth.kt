package com.tangem.datasource.api.common.config

import com.tangem.utils.Provider

internal class TangemVisaAuth : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.STAGE

    override val environmentConfigs = listOf(
        createStageEnvironment(),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
        baseUrl = "https://api-s.tangem.org/",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf<String, Provider<String>>()
}