package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.Instant

internal class PolymarketSoccerEventUpdateTest {
    @Test
    fun `GIVEN family and main markets WHEN main updates THEN preserve family totals structure and child markets`() {
        // Arrange
        val moneyline = threeWayCard().let { it.copy(markets = it.markets.map { m -> m.copy(eventId = "main") }) }
        val event = soccerEvent(listOf(moneyline, soccerPickerCard())).copy(
            volume = BigDecimal("1000"), volume24h = BigDecimal("200"), dataFetchedAt = Instant.ofEpochSecond(10),
        )
        val main = event.mainEvent().copy(
            volume = BigDecimal("100"), volume24h = BigDecimal("20"), isLive = false, isEnded = true,
            score = "2-1", teams = event.teams.reversed(), dataFetchedAt = Instant.ofEpochSecond(20),
            markets = moneyline.markets.map { it.copy(volume = BigDecimal("50")) },
        )

        // Act
        val actual = event.withMainEvent(main)

        // Assert
        val updatedTab = event.tabs.single().copy(cards = listOf(moneyline.copy(markets = main.markets), soccerPickerCard()))
        assertThat(actual).isEqualTo(event.copy(
            isLive = false, dataFetchedAt = main.dataFetchedAt, tabs = listOf(updatedTab),
        ))
        assertThat(actual.hasCurrentMoneyline(main)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "owner", "version", "asset"])
    fun `GIVEN incomplete or incompatible main market WHEN updated THEN do not rebind buttons or timestamp old prices`(
        change: String,
    ) {
        // Arrange
        val moneyline = threeWayCard().let { it.copy(markets = it.markets.map { m -> m.copy(eventId = "main") }) }
        val event = soccerEvent(listOf(moneyline)).copy(dataFetchedAt = Instant.ofEpochSecond(10))
        val market = moneyline.markets.first()
        val replacement = when (change) {
            "owner" -> market.copy(eventId = "other")
            "version" -> market.copy(version = PolymarketMarketVersion.V2)
            "asset" -> market.copy(outcomes = market.outcomes.map { it.copy(assetId = "new-${it.assetId}") })
            "missing" -> null
            else -> error("Unexpected case: $change")
        }
        val main = event.mainEvent().copy(
            dataFetchedAt = Instant.ofEpochSecond(20),
            markets = listOfNotNull(replacement) + moneyline.markets.drop(1),
        )

        // Act
        val actual = event.withMainEvent(main)

        // Assert
        assertThat(actual).isEqualTo(event)
        assertThat(actual.hasCurrentMoneyline(main)).isFalse()
    }
}