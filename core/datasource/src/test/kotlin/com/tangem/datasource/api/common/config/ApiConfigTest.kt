package com.tangem.datasource.api.common.config

import com.tangem.core.remote.config.ApiConfig

import com.google.common.truth.Truth
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.logging.TangemLogger
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiConfigTest {

    private val appAuthProvider = mockk<AuthProvider>()
    private val apiKeyProvider = mockk<ProviderSuspend<String>>()
    private val environmentConfig = mockk<EnvironmentConfig>()

    @BeforeEach
    fun setup() {
        clearMocks(
            appAuthProvider,
        )
        every { appAuthProvider.getApiKey(any()) } returns apiKeyProvider
    }

    @Test
    fun `all baseUrls ends with slash`() {
        // Arrange
        val allBaseUrls = createApiConfigs().flatMap { it.environmentConfigs.map { it.baseUrl } }

        // Actual
        val actual = allBaseUrls.all { it.endsWith("/") }

        TangemLogger.e(allBaseUrls.joinToString(separator = "\n"))

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    private fun createApiConfigs(): List<ApiConfig> {
        return listOf(
            Express(
                environmentConfig = environmentConfig,
                expressAuthProvider = mockk(),
                appInfoProvider = mockk(),
            ),
            YieldSupply(
                environmentConfig = environmentConfig,
                authProvider = appAuthProvider,
                appInfoProvider = mockk(),
            ),
            TangemTech(
                authProvider = appAuthProvider,
                appInfoProvider = mockk(),
            ),
            StakeKit(stakeKitAuthProvider = mockk()),
            BlockAid(environmentConfig = environmentConfig),
            MoonPay(),
            P2PEthPool(p2pAuthProvider = mockk()),
            News(
                authProvider = appAuthProvider,
                appInfoProvider = mockk(),
            ),
            GaslessTxService(
                authProvider = appAuthProvider,
                appInfoProvider = mockk(),
            ),
            SurveySparrow(environmentConfig = environmentConfig),
            Auth(),
        )
    }
}