package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

internal class PolymarketEventResponseTest {
    private val adapter = Moshi.Builder().build().adapter(PolymarketEventResponse::class.java)

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["soccer", "basketball", "future_type"])
    fun `GIVEN an optional event type WHEN decoding THEN its value is preserved`(type: String?) {
        // Arrange
        val discriminator = type?.let { ",\"eventType\":\"$it\"" }.orEmpty()
        val json = """
            {"event": {
              "eventId":"match", "slug":"match", "title":"Match", "description":"Rules",
              "polymarketRulesUrl":"https://polymarket.com", "icon":null, "image":null,
              "status":"active", "startDate":null, "endDate":null,
              "volume":null, "volume24hr":null, "liquidity":null,
              "totalMarketsCount":0, "negRisk":false, "live":false,
              "displayMode":"grouped_outcomes", "markets":[] $discriminator
            }}
        """.trimIndent()

        // Act
        val actual = requireNotNull(adapter.fromJson(json))

        // Assert
        assertThat(actual.event.eventType).isEqualTo(type)
    }
}