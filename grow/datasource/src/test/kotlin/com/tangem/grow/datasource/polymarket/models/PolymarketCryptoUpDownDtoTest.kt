package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class PolymarketCryptoUpDownDtoTest {

    private val adapter = Moshi.Builder().build().adapter(PolymarketEventResponse::class.java)

    @Test
    fun `GIVEN event details of a crypto window WHEN decoded THEN the block is read with an incomplete neighbour`() {
        // Arrange
        val json = eventJson(
            cryptoUpDown = """
                {
                  "duration": "5m",
                  "windowStart": "2026-01-01T10:00:00Z",
                  "priceFeed": {
                    "channel": "price.crypto.twap",
                    "filter": { "symbol": "coinusd", "window_seconds": 60 }
                  },
                  "priceToBeat": 123.456,
                  "finalPrice": null,
                  "nextEvents": [
                    { "id": "9000002", "windowStart": "2026-01-01T10:05:00Z", "windowEnd": "2026-01-01T10:10:00Z" },
                    { "id": "9000003", "windowStart": "2026-01-01T10:10:00Z" }
                  ],
                  "liveEventId": "9000001"
                }
            """.trimIndent(),
        )

        // Act
        val actual = adapter.fromJson(json)?.event?.cryptoUpDown

        // Assert
        val expected = PolymarketCryptoUpDownDto(
            windowStart = "2026-01-01T10:00:00Z",
            priceFeed = PolymarketCryptoPriceFeedDto(
                channel = "price.crypto.twap",
                filter = PolymarketCryptoPriceFeedFilterDto(symbol = "coinusd", provider = null, windowSeconds = 60),
            ),
            priceToBeat = 123.456,
            finalPrice = null,
            nextEvents = listOf(
                PolymarketCryptoNextEventDto(
                    id = "9000002",
                    windowStart = "2026-01-01T10:05:00Z",
                    windowEnd = "2026-01-01T10:10:00Z",
                ),
                PolymarketCryptoNextEventDto(id = "9000003", windowStart = "2026-01-01T10:10:00Z", windowEnd = null),
            ),
            liveEventId = "9000001",
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN feed item of a crypto window WHEN decoded THEN neighbours and the live event are absent`() {
        // Arrange
        val json = eventJson(
            cryptoUpDown = """
                {
                  "duration": "1h",
                  "windowStart": "2026-01-01T10:00:00Z",
                  "priceFeed": { "channel": "price.crypto", "filter": { "symbol": "coinusd", "provider": "binance" } },
                  "priceToBeat": 123.45,
                  "finalPrice": null,
                  "nextEvents": null,
                  "liveEventId": null
                }
            """.trimIndent(),
        )

        // Act
        val actual = adapter.fromJson(json)?.event?.cryptoUpDown

        // Assert
        val expected = PolymarketCryptoUpDownDto(
            windowStart = "2026-01-01T10:00:00Z",
            priceFeed = PolymarketCryptoPriceFeedDto(
                channel = "price.crypto",
                filter = PolymarketCryptoPriceFeedFilterDto(
                    symbol = "coinusd",
                    provider = "binance",
                    windowSeconds = null,
                ),
            ),
            priceToBeat = 123.45,
            finalPrice = null,
            nextEvents = null,
            liveEventId = null,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN event without the block WHEN decoded THEN it has no crypto window`() {
        // Act
        val actual = adapter.fromJson(eventJson(cryptoUpDown = null))?.event

        // Assert
        assertThat(actual?.eventId).isEqualTo("9000001")
        assertThat(actual?.cryptoUpDown).isNull()
    }

    private fun eventJson(cryptoUpDown: String?): String = """
        {
          "event": {
            "eventId": "9000001",
            "slug": "coin-updown-5m",
            "title": "Coin Up or Down",
            "description": "Event description",
            "polymarketRulesUrl": "https://polymarket.com/rules",
            "icon": null,
            "image": null,
            "status": "active",
            "startDate": "2025-12-31T10:00:00Z",
            "endDate": "2026-01-01T10:05:00Z",
            "volume": null,
            "volume24hr": null,
            "liquidity": null,
            "totalMarketsCount": 1,
            "negRisk": false,
            "live": false,
            "displayMode": "plain_markets",
            "eventType": "crypto",
            "markets": []
            ${cryptoUpDown?.let { ", \"cryptoUpDown\": $it" }.orEmpty()}
          }
        }
    """.trimIndent()
}