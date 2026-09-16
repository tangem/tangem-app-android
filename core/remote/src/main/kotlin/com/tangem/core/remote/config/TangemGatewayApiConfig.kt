package com.tangem.core.remote.config

import com.tangem.utils.ProviderSuspend

/**
 * Base [ApiConfig] for APIs served by the shared Tangem gateway (api.tangem.org). Concrete configs
 * extend it to inherit the per-environment base URL and declare only their own id, environments and
 * headers, so the gateway host is defined once here instead of copied into every config.
 */
abstract class TangemGatewayApiConfig : ApiConfig() {

    protected fun gatewayEnvironment(
        environment: ApiEnvironment,
        headers: Map<String, ProviderSuspend<String>> = emptyMap(),
    ): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = environment,
        baseUrl = gatewayBaseUrl(environment),
        headers = headers,
    )

    companion object {

        /**
         * Id of the primary Tangem gateway config (TangemTech). It lives on the shared gateway base
         * so a Retrofit API in any stream can attach to that config by id
         * (`RetrofitApiSpec(apiConfigId = TANGEM_TECH_ID, …)`) without depending on the module that
         * owns the config implementation.
         */
        const val TANGEM_TECH_KEY = "TangemTech"
        val TANGEM_TECH_ID = ApiConfig.ID(TANGEM_TECH_KEY)

        private fun gatewayBaseUrl(environment: ApiEnvironment): String = when (environment) {
            ApiEnvironment.PROD -> "https://api.tangem.org/"
            ApiEnvironment.DEV,
            ApiEnvironment.DEV_2,
            ApiEnvironment.DEV_3,
            -> "[REDACTED_ENV_URL]"
            ApiEnvironment.STAGE,
            ApiEnvironment.STAGE_2,
            ApiEnvironment.STAGE_3,
            -> "[REDACTED_ENV_URL]"
            ApiEnvironment.MOCK -> "[REDACTED_ENV_URL]"
        }
    }
}