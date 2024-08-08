package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig

/**
 * TangemTech [ApiConfig]
 *
 * @property currentEnvironment current api environment
 */
internal data class TangemTech(
    override val currentEnvironment: ApiEnvironment = initializeCurrentEnvironment(),
) : ApiConfig(currentEnvironment) {

    override val environments: Map<ApiEnvironment, String> = mapOf(
        ApiEnvironment.DEV to "https://devapi.tangem-tech.com/v1/",
        ApiEnvironment.PROD to "https://api.tangem-tech.com/v1/",
    )

    private companion object {

        fun initializeCurrentEnvironment(): ApiEnvironment {
            return when (BuildConfig.BUILD_TYPE) {
                DEBUG_BUILD_TYPE,
                INTERNAL_BUILD_TYPE,
                -> ApiEnvironment.DEV
                MOCKED_BUILD_TYPE,
                EXTERNAL_BUILD_TYPE,
                RELEASE_BUILD_TYPE,
                -> ApiEnvironment.PROD
                else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
            }
        }
    }
}