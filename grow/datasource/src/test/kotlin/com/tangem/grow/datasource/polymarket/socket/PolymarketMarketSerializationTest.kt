package com.tangem.grow.datasource.polymarket.socket

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PolymarketMarketSerializationTest {

    private val json = NetworkJsonFactory.create(
        serializersModule = PolymarketMarketSocketModule.providePolymarketMarketSerializersModule().provide(),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Decode {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a market frame WHEN decoded THEN wire values and event type are preserved`(model: DecodeModel) {
            // Arrange
            val body = model.body

            // Act
            val actual = json.decodeFromString<PolymarketMarketEventDto>(body)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<DecodeModel> = listOf(
            DecodeModel(
                body = """
                    {"event_type":"book","asset_id":"123456789012345678901234567890","market":"condition",
                     "bids":[{"price":"0.123456789012345678","size":"12.5000"}],"asks":[],
                     "timestamp":"1760000000000","hash":"ignored"}
                """.trimIndent(),
                expected = PolymarketMarketEventDto.Book(
                    assetId = "123456789012345678901234567890",
                    bids = listOf(PolymarketMarketEventDto.Level(price = "0.123456789012345678", size = "12.5000")),
                    asks = emptyList(),
                    timestamp = "1760000000000",
                ),
            ),
            DecodeModel(
                body = """{"event_type":"book","asset_id":"a","bids":[],"asks":[],"timestamp":"1"}""",
                expected = PolymarketMarketEventDto.Book("a", emptyList(), emptyList(), "1"),
            ),
            DecodeModel(
                body = """
                    {"event_type":"price_change","timestamp":"2","price_changes":[
                      {"asset_id":"a","best_bid":"0.42","best_ask":"0.58","side":"BUY","size":"0"},
                      {"asset_id":"b","best_ask":"0.62"},
                      {"asset_id":"c","best_bid":null,"best_ask":null}
                    ]}
                """.trimIndent(),
                expected = PolymarketMarketEventDto.PriceChange(
                    priceChanges = listOf(
                        PolymarketMarketEventDto.Change("a", "0.42", "0.58"),
                        PolymarketMarketEventDto.Change("b", bestAsk = "0.62"),
                        PolymarketMarketEventDto.Change("c"),
                    ),
                    timestamp = "2",
                ),
            ),
            DecodeModel(
                body = """
                    {"event_type":"best_bid_ask","asset_id":"a","best_bid":"0","best_ask":"1",
                     "spread":"1","timestamp":"3"}
                """.trimIndent(),
                expected = PolymarketMarketEventDto.BestBidAsk("a", "0", "1", "3"),
            ),
            DecodeModel(
                body = """
                    {"event_type":"last_trade_price","asset_id":"a","price":"0.500000000000000001",
                     "side":"SELL","timestamp":"4"}
                """.trimIndent(),
                expected = PolymarketMarketEventDto.LastTradePrice("a", "0.500000000000000001", "4"),
            ),
            DecodeModel(
                body = """{"event_type":"BOOK","asset_id":"a","bids":[],"asks":[],"timestamp":"5"}""",
                expected = PolymarketMarketEventDto.Book("a", emptyList(), emptyList(), "5"),
            ),
        ) + listOf("tick_size_change", "future_event", null).map { type ->
            val body = if (type == null) """{"asset_id":"a"}""" else """{"event_type":"$type","asset_id":"a"}"""
            DecodeModel(body, PolymarketMarketEventDto.Unknown(type, json.parseToJsonElement(body).jsonObject))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Encode {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a subscription command WHEN encoded THEN it matches the market protocol`(model: EncodeModel) {
            // Arrange
            val assetIds = listOf("a", "b")

            // Act
            val actual = if (model.operation == null) {
                json.encodeToString(PolymarketMarketSubscriptionDto(assetIds))
            } else {
                json.encodeToString(PolymarketMarketSubscriptionUpdateDto(assetIds, model.operation))
            }

            // Assert
            assertThat(json.parseToJsonElement(actual)).isEqualTo(json.parseToJsonElement(model.expected))
        }

        private fun provideTestModels() = listOf(
            EncodeModel(
                operation = null,
                expected = """{"assets_ids":["a","b"],"type":"market","custom_feature_enabled":true}""",
            ),
            EncodeModel(
                operation = PolymarketMarketSubscriptionUpdateDto.Operation.SUBSCRIBE,
                expected = """{"assets_ids":["a","b"],"operation":"subscribe","custom_feature_enabled":true}""",
            ),
            EncodeModel(
                operation = PolymarketMarketSubscriptionUpdateDto.Operation.UNSUBSCRIBE,
                expected = """{"assets_ids":["a","b"],"operation":"unsubscribe"}""",
            ),
        )
    }

    data class DecodeModel(val body: String, val expected: PolymarketMarketEventDto)

    data class EncodeModel(val operation: PolymarketMarketSubscriptionUpdateDto.Operation?, val expected: String)
}