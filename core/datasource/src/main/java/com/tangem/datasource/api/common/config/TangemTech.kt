package com.tangem.datasource.api.common.config

/** TangemTech [ApiConfig] */
internal class TangemTech : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environments: Map<ApiEnvironment, String> = mapOf(
        ApiEnvironment.DEV to "https://devapi.tangem-tech.com/v1/",
        ApiEnvironment.PROD to "https://api.tangem-tech.com/v1/",
    )
}
