package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider

/** TangemTech [ApiConfig] */
internal class TangemTech(
    private val appVersionProvider: AppVersionProvider,
    private val authProvider: AuthProvider,
    private val appInfoProvider: AppInfoProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs = listOf(
        createDevEnvironment(),
        createStageEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
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

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
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
        baseUrl = "https://api.tangem.org/",
        headers = createHeaders(),
    )

    private fun createHeaders() = buildMap {
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appVersionProvider, appInfoProvider).values)
        putAll(from = RequestHeader.AuthenticationHeader(authProvider).values)
    }
}