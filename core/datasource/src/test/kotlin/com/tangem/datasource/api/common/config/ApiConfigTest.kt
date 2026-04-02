package com.tangem.datasource.api.common.config

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

    private fun createApiConfigs(): ApiConfigs {
        return ApiConfig.ID.entries.mapTo(destination = hashSetOf()) {
            when (it) {
                ApiConfig.ID.Express -> {
                    Express(
                        environmentConfig = environmentConfig,
                        expressAuthProvider = mockk(),
                        appVersionProvider = mockk(),
                        appInfoProvider = mockk(),
                    )
                }
                ApiConfig.ID.YieldSupply -> {
                    YieldSupply(
                        environmentConfig = environmentConfig,
                        appVersionProvider = mockk(),
                        authProvider = appAuthProvider,
                        appInfoProvider = mockk(),
                    )
                }
                ApiConfig.ID.TangemTech -> {
                    TangemTech(
                        appVersionProvider = mockk(),
                        authProvider = appAuthProvider,
                        appInfoProvider = mockk(),
                    )
                }
                ApiConfig.ID.StakeKit -> StakeKit(stakeKitAuthProvider = mockk())
                ApiConfig.ID.TangemPay -> TangemPay.Bff(
                    environmentConfig = environmentConfig,
                    appVersionProvider = mockk(),
                )
                ApiConfig.ID.TangemPayAuth -> TangemPay.Auth(
                    environmentConfig = environmentConfig,
                    appVersionProvider = mockk(),
                )
                ApiConfig.ID.BlockAid -> BlockAid(environmentConfig = environmentConfig)
                ApiConfig.ID.MoonPay -> MoonPay()
                ApiConfig.ID.P2PEthPool -> P2PEthPool(p2pAuthProvider = mockk())
                ApiConfig.ID.News -> News(
                    appVersionProvider = mockk(),
                    authProvider = appAuthProvider,
                    appInfoProvider = mockk(),
                )
                ApiConfig.ID.GaslessTxService -> GaslessTxService(
                    authProvider = appAuthProvider,
                    appVersionProvider = mockk(),
                    appInfoProvider = mockk(),
                )
            }
        }
    }
}