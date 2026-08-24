package com.tangem.store.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.config.TangemGatewayApiConfig
import com.tangem.core.remote.header.RequestHeader
import com.tangem.core.remote.header.TangemApiKeyHeaderProvider
import com.tangem.store.datasource.BuildConfig
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider

class News(
    private val apiKeyHeader: TangemApiKeyHeaderProvider,
    private val appInfoProvider: AppInfoProvider,
) : TangemGatewayApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        gatewayEnvironment(ApiEnvironment.PROD, createHeaders(ApiEnvironment.PROD)),
        gatewayEnvironment(ApiEnvironment.DEV, createHeaders(ApiEnvironment.DEV)),
        gatewayEnvironment(ApiEnvironment.MOCK, createHeaders(ApiEnvironment.MOCK)),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            DEBUG_BUILD_TYPE,
            -> ApiEnvironment.DEV
            INTERNAL_BUILD_TYPE,
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createHeaders(environment: ApiEnvironment): Map<String, ProviderSuspend<String>> = buildMap {
        putAll(apiKeyHeader.forEnvironment(Provider { environment }).values)
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
    }

    companion object {

        const val KEY = "News"
        val ID = ApiConfig.ID(KEY)
    }
}