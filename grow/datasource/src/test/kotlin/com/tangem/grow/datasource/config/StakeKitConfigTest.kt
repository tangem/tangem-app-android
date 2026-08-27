package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StakeKitConfigTest {

    private val growEnvironmentConfig = mockk<GrowEnvironmentConfig>()

    private val config = StakeKit(growEnvironmentConfig = growEnvironmentConfig)

    @BeforeEach
    fun setup() {
        clearMocks(growEnvironmentConfig)

        every { growEnvironmentConfig.stakeKitApiKey } returns STAKE_KIT_API_KEY
    }

    @Test
    fun `GIVEN config WHEN read id THEN StakeKit id`() {
        // Assert
        assertThat(config.id).isEqualTo(StakeKit.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://api.stakek.it/v1/",
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
                "X-API-KEY" to STAKE_KIT_API_KEY,
                "accept" to "application/json",
            ),
        )
    }

    private companion object {
        const val STAKE_KIT_API_KEY = "stake_kit_api_key"
    }
}