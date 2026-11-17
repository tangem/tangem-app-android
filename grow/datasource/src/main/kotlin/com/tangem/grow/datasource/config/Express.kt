package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.header.RequestHeader
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.express.ExpressAuthProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider

/**
 * Express [ApiConfig].
 *
 * Carries no api key: Express `/v2` is authorized per method with a DPoP-bound session via
 * `com.tangem.lib.auth.api.RequiresSessionAuth`.
 */
class Express(
    private val expressAuthProvider: ExpressAuthProvider,
    private val appInfoProvider: AppInfoProvider,
) : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createEnvironment(ApiEnvironment.MOCK, host = "wiremock.tests-d.com"),
        createEnvironment(ApiEnvironment.PROD, host = "express.tangem.com"),
        createEnvironment(ApiEnvironment.DEV, host = "express-dev1.tests-d.com"),
        createEnvironment(ApiEnvironment.DEV_2, host = "express-dev2.tests-d.com"),
        createEnvironment(ApiEnvironment.DEV_3, host = "express-dev3.tests-d.com"),
        createEnvironment(ApiEnvironment.DEV_4, host = "express-dev4.tests-d.com"),
        createEnvironment(ApiEnvironment.DEV_5, host = "express-dev5.tests-d.com"),
        createEnvironment(ApiEnvironment.DEV_6, host = "express-dev6.tests-d.com"),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            DEBUG_BUILD_TYPE,
            INTERNAL_BUILD_TYPE,
            -> ApiEnvironment.DEV_6
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createEnvironment(environment: ApiEnvironment, host: String): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            environment = environment,
            baseUrl = "https://$host/v2/",
            headers = createHeaders(),
        )
    }

    private fun createHeaders() = buildMap {
        put(key = "session-id", value = ProviderSuspend(expressAuthProvider::getSessionId))
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
    }

    companion object {
        const val KEY = "Express"
        val ID = ApiConfig.ID(KEY)
    }
}