package com.tangem.datasource.api.common.config

/** TangemTech [ApiConfig] */
internal class TangemTech : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createDevEnvironment(),
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://api.tangem-tech.com/v1/",
        headers = emptyMap(), // TODO: [REDACTED_JIRA]
    )

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "https://devapi.tangem-tech.com/v1/",
        headers = emptyMap(), // TODO: [REDACTED_JIRA]
    )
}