package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.version.AppVersionProvider

internal sealed class TangemPay(
    private val appVersionProvider: AppVersionProvider,
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs = listOf(
        createDevEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
    )

    protected abstract fun getBaseUrl(apiEnvironment: ApiEnvironment): String

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
        baseUrl = getBaseUrl(ApiEnvironment.DEV),
        headers = createHeaders(ApiEnvironment.DEV),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = getBaseUrl(ApiEnvironment.MOCK),
        headers = createHeaders(ApiEnvironment.MOCK),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = getBaseUrl(ApiEnvironment.PROD),
        headers = createHeaders(ApiEnvironment.PROD),
    )

    private fun createHeaders(apiEnvironment: ApiEnvironment) = mapOf(
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "Android" },
        "X-API-KEY" to ProviderSuspend { getBffStaticToken(apiEnvironment) },
    )

    private fun getBffStaticToken(apiEnvironment: ApiEnvironment): String {
        return when (apiEnvironment) {
            ApiEnvironment.MOCK,
            ApiEnvironment.DEV,
            -> environmentConfigStorage.getConfigSync().bffStaticTokenDev
            ApiEnvironment.PROD -> environmentConfigStorage.getConfigSync().bffStaticToken
            ApiEnvironment.STAGE,
            ApiEnvironment.STAGE_2,
            ApiEnvironment.DEV_2,
            ApiEnvironment.DEV_3,
            -> null
        } ?: error("BffStaticToken is not provided for $apiEnvironment")
    }

    class Bff(
        appVersionProvider: AppVersionProvider,
        environmentConfigStorage: EnvironmentConfigStorage,
    ) : TangemPay(appVersionProvider, environmentConfigStorage) {
        override fun getBaseUrl(apiEnvironment: ApiEnvironment): String {
            return when (apiEnvironment) {
                ApiEnvironment.DEV -> "https://api.dev.us.paera.com/bff-v2/"
                ApiEnvironment.MOCK -> "[REDACTED_ENV_URL]"
                ApiEnvironment.PROD -> "https://api.us.paera.com/bff-v2/"
                ApiEnvironment.DEV_2,
                ApiEnvironment.DEV_3,
                ApiEnvironment.STAGE,
                ApiEnvironment.STAGE_2,
                -> error("Unknown environment: $apiEnvironment")
            }
        }
    }

    class Auth(
        appVersionProvider: AppVersionProvider,
        environmentConfigStorage: EnvironmentConfigStorage,
    ) : TangemPay(appVersionProvider, environmentConfigStorage) {
        override fun getBaseUrl(apiEnvironment: ApiEnvironment): String {
            return when (apiEnvironment) {
                ApiEnvironment.DEV -> "https://api.dev.us.paera.com/"
                ApiEnvironment.MOCK -> "[REDACTED_ENV_URL]"
                ApiEnvironment.PROD -> "https://api.us.paera.com/"
                ApiEnvironment.DEV_2,
                ApiEnvironment.DEV_3,
                ApiEnvironment.STAGE,
                ApiEnvironment.STAGE_2,
                -> error("Unknown environment: $apiEnvironment")
            }
        }
    }
}