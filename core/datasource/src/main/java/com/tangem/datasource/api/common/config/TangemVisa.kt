package com.tangem.datasource.api.common.config

import com.tangem.utils.ProviderSuspend
import com.tangem.utils.version.AppVersionProvider

internal class TangemVisa(
    private val appVersionProvider: AppVersionProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://bff.tangem.com/",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf(
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "Android" },
    )
}