package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.version.AppVersionProvider

internal class TangemPay(
    private val appVersionProvider: AppVersionProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs = listOf(
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
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://api.paera.com/bff/",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf(
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "Android" },
    )
}