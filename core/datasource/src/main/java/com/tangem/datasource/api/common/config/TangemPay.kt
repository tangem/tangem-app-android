package com.tangem.datasource.api.common.config

import com.tangem.utils.ProviderSuspend
import com.tangem.utils.version.AppVersionProvider

internal class TangemPay(
    private val appVersionProvider: AppVersionProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.DEV

    override val environmentConfigs = listOf(
        createProdEnvironment(),
        createDevEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://api.paera.com/bff/",
        headers = createHeaders(),
    )

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "https://api.dev.paera.com/bff/",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf(
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "Android" },
    )
}
