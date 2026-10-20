package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PolymarketPositionMatchingTest {

    private val yes = outcome(assetId = "yes-v2", legacyAssetId = "yes-v1")
    private val no = outcome(assetId = "no-v2", legacyAssetId = null)
    private val market = market(conditionId = "0xABC", outcomes = listOf(yes, no))
    private val other = market(conditionId = "0xDEF", outcomes = listOf(outcome(assetId = "other", legacyAssetId = null)))
    private val event = event(markets = listOf(other, market))

    @Nested
    inner class FindMarket {

        @Test
        fun `GIVEN a condition id in another case WHEN findMarket THEN the market with that condition is found`() {
            // Arrange
            val position = position(conditionId = "0xabc", assetId = "unknown")

            // Act
            val found = event.findMarket(position)

            // Assert
            assertThat(found).isSameInstanceAs(market)
        }

        @Test
        fun `GIVEN an unknown condition id WHEN findMarket THEN the market holding the legacy token is found`() {
            // Arrange
            val position = position(conditionId = "0x999", assetId = "yes-v1")

            // Act
            val found = event.findMarket(position)

            // Assert
            assertThat(found).isSameInstanceAs(market)
        }

        @Test
        fun `GIVEN empty ids WHEN findMarket THEN nothing is matched`() {
            // Arrange
            val blank = market(conditionId = "", outcomes = listOf(outcome(assetId = "", legacyAssetId = "")))
            val position = position(conditionId = "", assetId = "")

            // Act
            val found = event(markets = listOf(blank)).findMarket(position)

            // Assert
            assertThat(found).isNull()
        }
    }

    @Nested
    inner class FindOutcome {

        @Test
        fun `GIVEN a token of one outcome and the index of another WHEN findOutcome THEN the token wins`() {
            // Arrange
            val position = position(conditionId = "0xABC", assetId = "yes-v2", outcomeIndex = 1)

            // Act
            val found = market.findOutcome(position)

            // Assert
            assertThat(found).isSameInstanceAs(yes)
        }

        @Test
        fun `GIVEN a token no outcome carries WHEN findOutcome THEN the index decides`() {
            // Arrange
            val position = position(conditionId = "0xABC", assetId = "unknown", outcomeIndex = 1)

            // Act
            val found = market.findOutcome(position)

            // Assert
            assertThat(found).isSameInstanceAs(no)
        }

        @Test
        fun `GIVEN an index past the outcomes WHEN findOutcome THEN the legacy token decides`() {
            // Arrange
            val position = position(conditionId = "0xABC", assetId = "yes-v1", outcomeIndex = 5)

            // Act
            val found = market.findOutcome(position)

            // Assert
            assertThat(found).isSameInstanceAs(yes)
        }

        @Test
        fun `GIVEN no index and a current token WHEN findOutcome THEN the current token is preferred to a legacy one`() {
            // Arrange
            val shared = market(
                conditionId = "0xABC",
                outcomes = listOf(outcome(assetId = "a", legacyAssetId = "b"), outcome(assetId = "b", legacyAssetId = null)),
            )

            // Act
            val found = shared.findOutcome("b")

            // Assert
            assertThat(found?.assetId).isEqualTo("b")
        }

        @Test
        fun `GIVEN an empty asset id WHEN findOutcome THEN nothing is matched`() {
            // Arrange
            val blank = market(conditionId = "0xABC", outcomes = listOf(outcome(assetId = "", legacyAssetId = "")))

            // Act
            val found = blank.findOutcome("")

            // Assert
            assertThat(found).isNull()
        }
    }

    private fun outcome(assetId: String, legacyAssetId: String?) = PolymarketOutcome(
        assetId = assetId,
        title = "Outcome",
        probability = null,
        legacyAssetId = legacyAssetId,
    )

    private fun market(conditionId: String?, outcomes: List<PolymarketOutcome>): PolymarketMarket = mockk {
        every { this@mockk.conditionId } returns conditionId
        every { this@mockk.outcomes } returns outcomes
    }

    private fun event(markets: List<PolymarketMarket>): PolymarketEvent = mockk {
        every { this@mockk.markets } returns markets
    }

    private fun position(conditionId: String, assetId: String, outcomeIndex: Int? = null): PolymarketPosition.Open =
        mockk {
            every { id } returns PolymarketPositionId(conditionId = conditionId, assetId = assetId)
            every { this@mockk.outcomeIndex } returns outcomeIndex
        }
}