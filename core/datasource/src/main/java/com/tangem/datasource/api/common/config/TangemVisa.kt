package com.tangem.datasource.api.common.config

import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.utils.Provider
import kotlinx.coroutines.runBlocking

internal class TangemVisa(
    private val authProvider: TangemVisaAuthProvider,
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
        "Authorization" to Provider {
            // This is safe because it's used by interceptor which runs on the IO thread
            // (maybe change to ProviderSuspend implementation)
            runBlocking { authProvider.getAuthHeader() }
        },
    )
}