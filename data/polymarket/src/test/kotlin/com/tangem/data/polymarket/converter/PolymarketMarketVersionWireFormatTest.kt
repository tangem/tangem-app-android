package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.grow.datasource.polymarket.models.PolymarketEventResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class PolymarketMarketVersionWireFormatTest {

    private val moshi = Moshi.Builder().build()

    @Nested
    inner class Market {

        @Test
        fun `GIVEN a V2 market moved from V1 WHEN decoded THEN its version and legacy asset ids are read`() {
            // Arrange
            val json = eventJson(
                market = """
                    "version":"v2",
                    "outcomes":[
                      {"assetId":"895257305792","legacyAssetId":"1062291234","label":"Yes","probability":0.12},
                      {"assetId":"895257305793","legacyAssetId":"","label":"No","probability":0.88}
                    ]
                """.trimIndent(),
            )

            // Act
            val actual = decodeMarket(json)

            // Assert
            assertThat(actual.version).isEqualTo(PolymarketMarketVersion.V2)
            assertThat(actual.outcomes).containsExactly(
                PolymarketOutcome(
                    assetId = "895257305792",
                    title = "Yes",
                    probability = BigDecimal("0.12"),
                    legacyAssetId = "1062291234",
                ),
                PolymarketOutcome(
                    assetId = "895257305793",
                    title = "No",
                    probability = BigDecimal("0.88"),
                    legacyAssetId = "",
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN a market from a BFF without V2 fields WHEN decoded THEN its version is unknown`() {
            // Arrange
            val json = eventJson(market = """"outcomes":[{"assetId":"1","label":"Yes","probability":0.5}]""")

            // Act
            val actual = decodeMarket(json)

            // Assert
            assertThat(actual.version).isEqualTo(PolymarketMarketVersion.UNKNOWN)
            assertThat(actual.outcomes.single().legacyAssetId).isNull()
        }

        private fun decodeMarket(json: String) = moshi.adapter(PolymarketEventResponse::class.java)
            .fromJson(json)!!
            .event
            .let(PolymarketEventConverter::convert)
            .markets
            .single()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Quote {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a quote's signing domain WHEN decoded THEN it is carried as reported`(model: QuoteModel) {
            // Arrange
            val json = quoteJson(model.json)

            // Act
            val actual = moshi.adapter(PolymarketOrderQuoteResponse::class.java)
                .fromJson(json)!!
                .let(PredictionOrderQuoteConverter::convert)

            // Assert
            assertThat(Triple(actual.version, actual.exchange, actual.signingDomainVersion)).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            QuoteModel(
                json = ""","version":"v2","exchange":"$EXCHANGE_V3","signingDomainVersion":"3"""",
                expected = Triple(PolymarketMarketVersion.V2, EXCHANGE_V3, "3"),
            ),
            QuoteModel(
                json = ""","version":"unknown","exchange":null,"signingDomainVersion":null""",
                expected = Triple(PolymarketMarketVersion.UNKNOWN, null, null),
            ),
            QuoteModel(json = "", expected = Triple(PolymarketMarketVersion.UNKNOWN, null, null)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Position {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an open position's market version WHEN decoded THEN unavailable stays apart from unknown`(
            model: PositionModel,
        ) {
            // Arrange
            val json = """{"position":${positionJson(model.json)}}"""

            // Act
            val actual = moshi.adapter(PolymarketPositionResponse::class.java)
                .fromJson(json)!!
                .position!!
                .let(PolymarketOpenPositionConverter::convert)

            // Assert
            assertThat(actual.version).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PositionModel(json = ""","version":"v1"""", expected = PolymarketMarketVersion.V1),
            PositionModel(json = ""","version":"v2"""", expected = PolymarketMarketVersion.V2),
            PositionModel(json = ""","version":"unknown"""", expected = PolymarketMarketVersion.UNKNOWN),
            PositionModel(json = ""","version":null""", expected = null),
            PositionModel(json = "", expected = null),
        )
    }

    private fun eventJson(market: String): String = """
        {"event":{
          "eventId":"event-1", "slug":"event", "title":"Event", "description":"Description",
          "polymarketRulesUrl":"https://polymarket.com/rules", "status":"active",
          "totalMarketsCount":1, "negRisk":false, "live":false, "displayMode":"plain_markets",
          "markets":[{
            "id":"559651", "conditionId":"0xcondition", "eventId":"event-1", "question":"Question",
            "slug":"market", "description":"Description", "status":"active", "negRisk":false,
            "acceptingOrders":true, "orderIndex":0,
            $market
          }]
        }}
    """.trimIndent()

    private fun quoteJson(signingDomain: String): String = """
        {"status":"FULL","side":"BUY","shares":"1","notional":"1","expectedExecutionAmount":"1",
        "averagePrice":"0.1","worstCasePrice":"0.1","fees":{"market":"0","builder":"0","total":"0"},
        "total":"1","builderCode":"0x","minOrderSize":"5","tickSize":"0.001","live":false$signingDomain}
    """.trimIndent()

    private fun positionJson(version: String): String = """
        {
          "conditionId":"condition-1", "assetId":"asset-1", "title":"Position",
          "outcome":"Yes", "outcomeIndex":0, "size":"10", "avgPrice":"0.3", "curPrice":"0.4",
          "initialValue":"3", "currentValue":"4", "cashPnl":"1", "percentPnl":"33.33",
          "realizedPnl":"0", "totalBought":"10", "redeemable":false, "negRisk":false,
          "acceptingOrders":true, "live":false, "resolutionStatus":"OPEN"
          $version
        }
    """.trimIndent()

    internal data class QuoteModel(val json: String, val expected: Triple<PolymarketMarketVersion, String?, String?>)

    internal data class PositionModel(val json: String, val expected: PolymarketMarketVersion?)

    private companion object {
        const val EXCHANGE_V3 = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa"
    }
}