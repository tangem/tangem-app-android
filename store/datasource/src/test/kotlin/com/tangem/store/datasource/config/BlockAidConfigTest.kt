package com.tangem.store.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BlockAidConfigTest {

    private val storeEnvironmentConfig = mockk<StoreEnvironmentConfig>()

    private val config = BlockAid(storeEnvironmentConfig)

    @BeforeEach
    fun setup() {
        clearMocks(storeEnvironmentConfig)

        every { storeEnvironmentConfig.blockAidApiKey } returns BLOCK_AID_API_KEY
    }

    @Test
    fun `GIVEN config WHEN read id THEN BlockAid id`() {
        // Assert
        assertThat(config.id).isEqualTo(BlockAid.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://api.blockaid.io/v0/",
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
                "X-API-KEY" to BLOCK_AID_API_KEY,
                "accept" to "application/json",
                "content-type" to "application/json",
            ),
        )
    }

    private companion object {
        const val BLOCK_AID_API_KEY = "block_aid_api_key"
    }
}