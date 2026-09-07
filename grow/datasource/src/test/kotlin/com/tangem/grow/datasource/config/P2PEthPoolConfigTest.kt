package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class P2PEthPoolConfigTest {

    private val growEnvironmentConfig = mockk<GrowEnvironmentConfig>()

    private val config = P2PEthPool(growEnvironmentConfig = growEnvironmentConfig)

    @BeforeEach
    fun setup() {
        clearMocks(growEnvironmentConfig)

        // mainnet and hoodi kept equal so the assertion is robust regardless of the compile-time
        // P2PEthPoolStakingConfig.USE_TESTNET flag that selects between them.
        every { growEnvironmentConfig.p2pApiKey } returns
            GrowEnvironmentConfig.P2PKeys(mainnet = P2P_API_KEY, hoodi = P2P_API_KEY)
    }

    @Test
    fun `GIVEN config WHEN read id THEN P2PEthPool id`() {
        // Assert
        assertThat(config.id).isEqualTo(P2PEthPool.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://api.p2p.org/",
                ApiEnvironment.DEV to "https://api-test.p2p.org/",
                ApiEnvironment.MOCK to "[REDACTED_ENV_URL]",
            ),
        )
    }

    @Test
    fun `GIVEN config WHEN read PROD headers THEN expected headers`() {
        // Arrange
        val prod = config.environmentConfigs.first { it.environment == ApiEnvironment.PROD }

        // Act
        val actual = prod.headers.mapValues { runBlocking { it.value() } }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                "Authorization" to "Bearer $P2P_API_KEY",
                "accept" to "application/json",
                "Content-Type" to "application/json",
            ),
        )
    }

    private companion object {
        const val P2P_API_KEY = "p2p_api_key"
    }
}