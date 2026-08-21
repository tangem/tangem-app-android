package com.tangem.lib.auth.api.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.libs.auth.BuildConfig

/**
 * Tangem Auth Service [ApiConfig] — endpoints for device registration, authentication,
 * nonce issuance, refresh token rotation, and JWKS publication. Reuses the shared gateway host
 * ([TangemGatewayApiConfig]); the auth endpoints carry no config-level headers.
 */
class Auth : TangemGatewayApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        gatewayEnvironment(ApiEnvironment.DEV),
        gatewayEnvironment(ApiEnvironment.PROD),
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

    companion object {

        const val KEY = "Auth"
        val ID = ApiConfig.ID(KEY)
    }
}