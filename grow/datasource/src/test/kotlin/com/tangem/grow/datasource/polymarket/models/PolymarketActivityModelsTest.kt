package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class PolymarketActivityModelsTest {

    private val moshi = Moshi.Builder().build()
    private val pageAdapter = moshi.adapter(PolymarketActivityPageResponse::class.java)

    @Test
    fun `GIVEN an order with market metadata WHEN decoded THEN precision and wire fields are preserved`() {
        // Arrange
        val json = """
            {
              "items": [{
                "id": "buy-1", "type": "BUY", "occurredAt": "2026-09-28T13:40:00Z",
                "amount": "166.845000000000000001", "transactionHash": "0xtransaction",
                "market": {
                  "conditionId": "0xcondition", "outcomeIndex": 1, "shares": "248.500000",
                  "outcome": "No", "marketId": "42", "slug": "market-slug", "title": "Market title",
                  "icon": "https://example.com/market-icon.png", "image": "https://example.com/market.png",
                  "endDate": "2026-09-30T00:00:00Z", "negRisk": true,
                  "event": {
                    "eventId": "7", "slug": "event-slug", "title": "Event title",
                    "icon": "https://example.com/event-icon.png", "image": "https://example.com/event.png",
                    "endDate": "2026-10-01T00:00:00Z"
                  }
                },
                "order": {
                  "assetId": "123456789012345678901234567890", "averagePrice": "0.670000",
                  "platformFee": "0.500000", "builderFee": "0.050000",
                  "notional": "166.295000000000000001", "tradeCount": 2
                },
                "redeem": null
              }],
              "nextCursor": "opaque+/cursor=="
            }
        """.trimIndent()
        val expected = PolymarketActivityPageResponse(
            items = listOf(
                PolymarketActivityDto(
                    id = "buy-1",
                    type = "BUY",
                    occurredAt = "2026-09-28T13:40:00Z",
                    amount = "166.845000000000000001",
                    transactionHash = "0xtransaction",
                    market = PolymarketActivityMarketDto(
                        conditionId = "0xcondition",
                        outcomeIndex = 1,
                        shares = "248.500000",
                        outcome = "No",
                        marketId = "42",
                        slug = "market-slug",
                        title = "Market title",
                        icon = "https://example.com/market-icon.png",
                        image = "https://example.com/market.png",
                        endDate = "2026-09-30T00:00:00Z",
                        isNegRisk = true,
                        event = PolymarketActivityEventDto(
                            eventId = "7",
                            slug = "event-slug",
                            title = "Event title",
                            icon = "https://example.com/event-icon.png",
                            image = "https://example.com/event.png",
                            endDate = "2026-10-01T00:00:00Z",
                        ),
                    ),
                    order = PolymarketActivityOrderDto(
                        assetId = "123456789012345678901234567890",
                        averagePrice = "0.670000",
                        platformFee = "0.500000",
                        builderFee = "0.050000",
                        notional = "166.295000000000000001",
                        tradeCount = 2,
                    ),
                ),
            ),
            nextCursor = "opaque+/cursor==",
        )

        // Act
        val actual = pageAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN funding without trade details WHEN decoded THEN omitted and null fields are accepted`() {
        // Arrange
        val json = """
            {
              "items": [
                {"id":"deposit-1","type":"DEPOSIT","occurredAt":"2026-09-28T13:40:00Z","amount":"5000.31"},
                {"id":"withdraw-1","type":"WITHDRAW","occurredAt":"2026-09-27T13:40:00Z","amount":"5000.00",
                 "transactionHash":null,"market":null,"order":null,"redeem":null}
              ],
              "nextCursor": null
            }
        """.trimIndent()
        val expected = PolymarketActivityPageResponse(
            items = listOf(
                PolymarketActivityDto(
                    id = "deposit-1", type = "DEPOSIT", occurredAt = "2026-09-28T13:40:00Z", amount = "5000.31",
                ),
                PolymarketActivityDto(
                    id = "withdraw-1", type = "WITHDRAW", occurredAt = "2026-09-27T13:40:00Z", amount = "5000.00",
                ),
            ),
        )

        // Act
        val actual = pageAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a redemption without market metadata WHEN decoded THEN optional metadata stays absent`() {
        // Arrange
        val json = """
            {
              "items": [{
                "id": "redeem-1", "type": "REDEEM", "occurredAt": "2026-09-28T13:40:00Z", "amount": "0",
                "market": {"conditionId":"0xcondition","outcomeIndex":0,"shares":"248.50"},
                "redeem": {"result":"LOST"}
              }]
            }
        """.trimIndent()
        val expected = PolymarketActivityPageResponse(
            items = listOf(
                PolymarketActivityDto(
                    id = "redeem-1",
                    type = "REDEEM",
                    occurredAt = "2026-09-28T13:40:00Z",
                    amount = "0",
                    market = PolymarketActivityMarketDto(
                        conditionId = "0xcondition", outcomeIndex = 0, shares = "248.50",
                    ),
                    redeem = PolymarketActivityRedeemDto(result = "LOST"),
                ),
            ),
        )

        // Act
        val actual = pageAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN an operation without an amount WHEN decoded THEN the response fails instead of inventing a value`() {
        // Arrange
        val json = """
            {"items":[{"id":"deposit-1","type":"DEPOSIT","occurredAt":"2026-09-28T13:40:00Z"}]}
        """.trimIndent()

        // Act
        val error = runCatching { pageAdapter.fromJson(json) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(JsonDataException::class.java)
        assertThat(error).hasMessageThat().contains("amount")
    }

    @Test
    fun `GIVEN a reconcile response WHEN decoded THEN the retry delay retains its int64 range`() {
        // Arrange
        val json = """{"state":"SCANNING","retryAfterSeconds":2147483648}"""

        // Act
        val actual = moshi.adapter(PolymarketDepositReconcileResponse::class.java).fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketDepositReconcileResponse(state = "SCANNING", retryAfterSeconds = 2147483648L),
        )
    }
}