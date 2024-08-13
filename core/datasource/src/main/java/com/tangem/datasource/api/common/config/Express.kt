package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig

/** Express [ApiConfig] */
internal class Express : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environments: Map<ApiEnvironment, String> = mapOf(
        ApiEnvironment.DEV to "https://express.tangem.org/v1/",
        ApiEnvironment.STAGE to "https://express-stage.tangem.com/v1/",
        ApiEnvironment.PROD to "https://express.tangem.com/v1/",
    )

    private companion object {

        fun getInitialEnvironment(): ApiEnvironment {
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
