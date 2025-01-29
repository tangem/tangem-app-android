package com.tangem.datasource.api.common.config

import com.tangem.utils.ProviderSuspend

internal class TangemVisa : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://bff.tangem.com/",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf<String, ProviderSuspend<String>>()
}