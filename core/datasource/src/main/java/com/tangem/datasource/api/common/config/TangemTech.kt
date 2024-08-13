package com.tangem.datasource.api.common.config

/**
 * TangemTech [ApiConfig]
 *
 * @property currentEnvironment current api environment
 */
internal data class TangemTech(
    override val currentEnvironment: ApiEnvironment = ApiEnvironment.PROD,
) : ApiConfig(currentEnvironment) {

    override val environments: Map<ApiEnvironment, String> = mapOf(
        ApiEnvironment.DEV to "https://devapi.tangem-tech.com/v1/",
        ApiEnvironment.PROD to "https://api.tangem-tech.com/v1/",
    )
}
