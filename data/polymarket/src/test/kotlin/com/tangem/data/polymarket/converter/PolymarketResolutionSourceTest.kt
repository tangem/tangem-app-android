package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.grow.datasource.polymarket.models.PolymarketClosedPositionDto
import com.tangem.grow.datasource.polymarket.models.PolymarketEventDto
import com.tangem.grow.datasource.polymarket.models.PolymarketOpenPositionDto
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketResolutionSourceTest {

    private val moshi = Moshi.Builder().build()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN optional source on the wire WHEN decoded and mapped THEN its value is preserved`(model: DecodeModel) {
        // Arrange
        val json = model.entity.json.replaceFirst("{", "{${model.field}")

        // Act
        val source = when (model.entity) {
            Entity.Event -> PolymarketEventConverter.convert(
                requireNotNull(moshi.adapter(PolymarketEventDto::class.java).fromJson(json)),
            ).resolutionSource
            Entity.OpenPosition -> PolymarketOpenPositionConverter.convert(
                requireNotNull(moshi.adapter(PolymarketOpenPositionDto::class.java).fromJson(json)),
            ).resolutionSource
            Entity.ClosedPosition -> PolymarketClosedPositionConverter.convert(
                requireNotNull(moshi.adapter(PolymarketClosedPositionDto::class.java).fromJson(json)),
            ).resolutionSource
        }

        // Assert
        assertThat(source).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<DecodeModel> = Entity.entries.flatMap { entity ->
        listOf(
            DecodeModel(entity, field = "", expected = null),
            DecodeModel(entity, field = "\"resolutionSource\":null,", expected = null),
            DecodeModel(entity, field = "\"resolutionSource\":\"\",", expected = ""),
            DecodeModel(entity, field = "\"resolutionSource\":\"  \",", expected = "  "),
            DecodeModel(entity, field = "\"resolutionSource\":\"FIFA\",", expected = "FIFA"),
            DecodeModel(
                entity,
                field = "\"resolutionSource\":\"https://www.binance.com/en/trade/BTC_USDT\",",
                expected = "https://www.binance.com/en/trade/BTC_USDT",
            ),
        )
    }

    internal data class DecodeModel(val entity: Entity, val field: String, val expected: String?)

    internal enum class Entity(val json: String) {
        Event(
            """{
                "eventId":"event-1","slug":"event","title":"Event","description":"Rules",
                "polymarketRulesUrl":"https://polymarket.com/rules","status":"active",
                "totalMarketsCount":0,"negRisk":false,"live":false,"displayMode":"plain_markets","markets":[]
            }""".trimIndent(),
        ),
        OpenPosition(
            """{
                "conditionId":"condition","assetId":"asset","title":"Market","outcome":"Yes",
                "size":"1","avgPrice":"0.5","curPrice":"0.6","initialValue":"0.5",
                "currentValue":"0.6","cashPnl":"0.1","percentPnl":"20","realizedPnl":"0",
                "totalBought":"1","redeemable":false,"negRisk":false,"acceptingOrders":true,
                "live":false,"resolutionStatus":"OPEN"
            }""".trimIndent(),
        ),
        ClosedPosition(
            """{
                "conditionId":"condition","assetId":"asset","title":"Market","outcome":"Yes",
                "avgPrice":"0.5","curPrice":"1","totalBought":"1","realizedPnl":"0.5",
                "closedAt":"2026-10-22T12:00:00Z","status":"WON"
            }""".trimIndent(),
        ),
    }
}