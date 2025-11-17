package com.tangem.datasource.api.common.config

import com.google.common.truth.Truth
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.utils.ProviderSuspend
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiConfigTest {

    private val appAuthProvider = mockk<AuthProvider>()
    private val apiKeyProvider = mockk<ProviderSuspend<String>>()

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

        Timber.e(allBaseUrls.joinToString(separator = "\n"))

        // Assert
        Truth.assertThat(actual).isTrue()
    }

    private fun createApiConfigs(): ApiConfigs {
        return ApiConfig.ID.entries.mapTo(destination = hashSetOf()) {
            when (it) {
                ApiConfig.ID.Express -> {
                    Express(
                        environmentConfigStorage = mockk(),
                        expressAuthProvider = mockk(),
                        appVersionProvider = mockk(),
                        appInfoProvider = mockk(),
                    )
                }
                ApiConfig.ID.YieldSupply -> {
                    YieldSupply(
                        environmentConfigStorage = mockk(),
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
                ApiConfig.ID.TangemPay -> TangemPay(appVersionProvider = mockk())
                ApiConfig.ID.BlockAid -> BlockAid(configStorage = mockk())
                ApiConfig.ID.MoonPay -> MoonPay()
            }
        }
    }
}