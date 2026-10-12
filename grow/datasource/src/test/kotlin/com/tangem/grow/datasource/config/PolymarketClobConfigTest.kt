package com.tangem.grow.datasource.config

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.config.ApiEnvironment
import org.junit.jupiter.api.Test

internal class PolymarketClobConfigTest {

    private val config = PolymarketClob()

    @Test
    fun `GIVEN config WHEN read id THEN PolymarketClob id`() {
        // Assert
        assertThat(config.id).isEqualTo(PolymarketClob.ID)
    }

    @Test
    fun `GIVEN config WHEN read environments THEN expected base urls`() {
        // Act
        val actual = config.environmentConfigs.associate { it.environment to it.baseUrl }

        // Assert
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                ApiEnvironment.PROD to "https://clob.polymarket.com/",
            ),
        )
    }
}