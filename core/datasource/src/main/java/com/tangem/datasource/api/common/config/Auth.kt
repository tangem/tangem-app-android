package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig

/**
 * Tangem Auth Service [ApiConfig] — endpoints for device registration, authentication,
 * nonce issuance, refresh token rotation, and JWKS publication.
 */
internal class Auth : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createDevEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE -> ApiEnvironment.MOCK
            DEBUG_BUILD_TYPE,
            INTERNAL_BUILD_TYPE,
            -> ApiEnvironment.DEV
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = DEV_BASE_URL,
        headers = emptyMap(),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = MOCK_BASE_URL,
        headers = emptyMap(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = PROD_BASE_URL,
        headers = emptyMap(),
    )

    private companion object {

        // TODO Replace with real Auth Service hosts once the backend team confirms deployment.
        //  Swagger currently only declares `http://localhost:8080` for local development.
        //  [REDACTED_JIRA]
        private const val DEV_BASE_URL = "http://localhost:8080/"
        private const val MOCK_BASE_URL = "http://localhost:8080/"
        private const val PROD_BASE_URL = "http://localhost:8080/"
    }
}