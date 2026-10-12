package com.tangem.store.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.header.CardAuthHeaderProvider
import com.tangem.core.remote.header.RequestHeader
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.store.datasource.BuildConfig
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider

/** TangemTech [ApiConfig] — the main Tangem gateway (api.tangem.org). */
class TangemTech(
    private val apiKeyHeader: TangemApiKeyHeaderProvider,
    private val cardAuthHeader: CardAuthHeaderProvider,
    private val appInfoProvider: AppInfoProvider,
) : TangemGatewayApiConfig() {

    override val id: ApiConfig.ID get() = TANGEM_TECH_ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        gatewayEnvironment(ApiEnvironment.DEV, createHeaders(ApiEnvironment.DEV)),
        gatewayEnvironment(ApiEnvironment.STAGE, createHeaders(ApiEnvironment.STAGE)),
        gatewayEnvironment(ApiEnvironment.MOCK, createHeaders(ApiEnvironment.MOCK)),
        gatewayEnvironment(ApiEnvironment.PROD, createHeaders(ApiEnvironment.PROD)),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            DEBUG_BUILD_TYPE,
            INTERNAL_BUILD_TYPE,
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createHeaders(environment: ApiEnvironment): Map<String, ProviderSuspend<String>> = buildMap {
        putAll(from = apiKeyHeader.forEnvironment(environment).values)
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
        putAll(from = cardAuthHeader.get().values)
    }
}