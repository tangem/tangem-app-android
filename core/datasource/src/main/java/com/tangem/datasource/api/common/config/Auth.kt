package com.tangem.datasource.api.common.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

import com.tangem.datasource.BuildConfig

/**
 * Tangem Auth Service [ApiConfig] — endpoints for device registration, authentication,
 * nonce issuance, refresh token rotation, and JWKS publication.
 */
internal class Auth : ApiConfig() {

    override val id: ApiConfig.ID = ApiConfig.ID.Auth

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createDevEnvironment(),
        createProdEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
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

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = PROD_BASE_URL,
        headers = emptyMap(),
    )

    companion object {

        // Same-module copy of the id key for use as a Dagger @StringKey argument (kapt can't use a
        // cross-module const). Kept in sync with the central [ApiConfig.AUTH].
        const val KEY = ApiConfig.AUTH

        private const val DEV_BASE_URL = "[REDACTED_ENV_URL]"
        private const val PROD_BASE_URL = "https://api.tangem.org/"
    }
}