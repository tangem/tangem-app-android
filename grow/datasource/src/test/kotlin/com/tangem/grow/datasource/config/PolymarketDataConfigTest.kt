package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import org.junit.jupiter.api.Test

internal class PolymarketDataConfigTest {

    private val config = PolymarketData()

    @Test
    fun `GIVEN config WHEN read id THEN PolymarketData id`() {
        // Assert
        assertThat(config.id).isEqualTo(PolymarketData.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://data-api.polymarket.com/",
            ),
        )
    }
}