package com.tangem.datasource.api.common.config

import com.tangem.utils.ProviderSuspend
import com.tangem.utils.version.AppVersionProvider

internal class TangemVisaAuth(
    private val appVersionProvider: AppVersionProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.STAGE

    override val environmentConfigs = listOf(
        createStageEnvironment(),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createHeaders() = mapOf(
        "version" to ProviderSuspend { appVersionProvider.versionName },
        "platform" to ProviderSuspend { "Android" },
    )
}