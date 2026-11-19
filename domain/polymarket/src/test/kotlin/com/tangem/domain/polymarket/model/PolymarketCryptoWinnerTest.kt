package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketCryptoWinnerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN ended window WHEN winner is read THEN settled outcome wins and prices tell it until then`(model: Model) {
        // Arrange
        val event = createEvent(
            outcomes = model.outcomes,
            window = createWindow(priceToBeat = model.priceToBeat, finalPrice = model.finalPrice),
        )

        // Act
        val actual = event.cryptoWinner()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model(
            outcomes = listOf(UP.copy(probability = BigDecimal.ZERO), DOWN.copy(probability = BigDecimal("1.0"))),
            priceToBeat = "100",
            finalPrice = "105",
            expected = PolymarketCryptoWinner(DOWN.copy(probability = BigDecimal("1.0")), isResolved = true),
        ),
        Model(
            outcomes = listOf(UP.copy(probability = BigDecimal.ONE), DOWN.copy(probability = BigDecimal.ZERO)),
            priceToBeat = null,
            finalPrice = null,
            expected = PolymarketCryptoWinner(UP.copy(probability = BigDecimal.ONE), isResolved = true),
        ),
        Model(priceToBeat = "100", finalPrice = "100.01", expected = PolymarketCryptoWinner(UP, isResolved = false)),
        Model(priceToBeat = "100", finalPrice = "100.00", expected = PolymarketCryptoWinner(UP, isResolved = false)),
        Model(priceToBeat = "100", finalPrice = "99.99", expected = PolymarketCryptoWinner(DOWN, isResolved = false)),
        Model(
            outcomes = listOf(DOWN.copy(title = "DOWN"), UP.copy(title = "up")),
            priceToBeat = "100",
            finalPrice = "105",
            expected = PolymarketCryptoWinner(UP.copy(title = "up"), isResolved = false),
        ),
        Model(
            outcomes = listOf(UP.copy(title = "Higher"), DOWN.copy(title = "Lower")),
            priceToBeat = "100",
            finalPrice = "105",
            expected = PolymarketCryptoWinner(UP.copy(title = "Higher"), isResolved = false),
        ),
        Model(
            outcomes = listOf(UP.copy(title = "Higher"), DOWN.copy(title = "Lower")),
            priceToBeat = "100",
            finalPrice = "95",
            expected = PolymarketCryptoWinner(DOWN.copy(title = "Lower"), isResolved = false),
        ),
        Model(
            outcomes = listOf(UP.copy(probability = BigDecimal("0.999")), DOWN.copy(probability = BigDecimal("0.001"))),
            priceToBeat = "100",
            finalPrice = null,
            expected = null,
        ),
        Model(priceToBeat = null, finalPrice = "105", expected = null),
        Model(outcomes = emptyList(), priceToBeat = "100", finalPrice = "105", expected = null),
    )

    @Test
    fun `GIVEN event without a window WHEN winner is read THEN there is none`() {
        // Arrange
        val event = createEvent(
            outcomes = listOf(UP.copy(probability = BigDecimal.ONE), DOWN.copy(probability = BigDecimal.ZERO)),
            window = null,
        )

        // Act
        val actual = event.cryptoWinner()

        // Assert
        assertThat(actual).isNull()
    }

    data class Model(
        val outcomes: List<PolymarketOutcome> = listOf(UP, DOWN),
        val priceToBeat: String?,
        val finalPrice: String?,
        val expected: PolymarketCryptoWinner?,
    )

    private fun createWindow(priceToBeat: String?, finalPrice: String?) = PolymarketCryptoWindow(
        start = Instant.ofEpochSecond(0),
        end = Instant.ofEpochSecond(300),
        priceFeed = null,
        priceToBeat = priceToBeat?.toBigDecimal(),
        finalPrice = finalPrice?.toBigDecimal(),
        nextWindows = emptyList(),
        liveEventId = null,
    )

    private fun createEvent(outcomes: List<PolymarketOutcome>, window: PolymarketCryptoWindow?) = PolymarketEvent(
        id = "event", slug = "event", title = "Bitcoin Up or Down", description = "", rulesUrl = "", iconUrl = null,
        imageUrl = null, status = PolymarketStatus.ACTIVE, startDate = null, endDate = null, volume = null,
        volume24h = null, liquidity = null, totalMarketsCount = 1, isNegRisk = false, isLive = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS, markets = listOf(createMarket(outcomes)),
        cryptoWindow = window,
    )

    private fun createMarket(outcomes: List<PolymarketOutcome>) = PolymarketMarket(
        id = "market", conditionId = null, eventId = "event", title = "Bitcoin Up or Down", slug = "market",
        description = "", groupItemTitle = null, iconUrl = null, imageUrl = null, status = PolymarketStatus.ACTIVE,
        isNegRisk = false, isAcceptingOrders = true, startDate = null, endDate = null, startDateIso = null,
        endDateIso = null, volume = null, volume24h = null, liquidity = null, orderIndex = 0, outcomes = outcomes,
        version = PolymarketMarketVersion.V2,
    )

    private companion object {
        val UP = PolymarketOutcome(assetId = "up", title = "Up", probability = null, legacyAssetId = null)
        val DOWN = PolymarketOutcome(assetId = "down", title = "Down", probability = null, legacyAssetId = null)
    }
}