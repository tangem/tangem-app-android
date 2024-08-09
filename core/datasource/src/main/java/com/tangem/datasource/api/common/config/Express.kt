package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig

/**
 * Express [ApiConfig]
 *
 * @property currentEnvironment current api environment
 */
internal data class Express(
    override val currentEnvironment: ApiEnvironment = initializeCurrentEnvironment(),
) : ApiConfig(currentEnvironment) {

    override val environments: Map<ApiEnvironment, String> = mapOf(
        ApiEnvironment.DEV to "[REDACTED_ENV_URL]",
        ApiEnvironment.STAGE to "[REDACTED_ENV_URL]",
        ApiEnvironment.PROD to "[REDACTED_ENV_URL]",
    )

    private companion object {

        fun initializeCurrentEnvironment(): ApiEnvironment {
            return when (BuildConfig.BUILD_TYPE) {
                DEBUG_BUILD_TYPE -> ApiEnvironment.DEV
                INTERNAL_BUILD_TYPE,
                MOCKED_BUILD_TYPE,
                -> ApiEnvironment.STAGE
                EXTERNAL_BUILD_TYPE,
                RELEASE_BUILD_TYPE,
                -> ApiEnvironment.PROD
                else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
            }
        }
    }
}