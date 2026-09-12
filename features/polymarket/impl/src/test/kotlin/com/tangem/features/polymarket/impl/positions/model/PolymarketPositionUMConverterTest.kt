package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPositionUMConverterTest {

    private val converter = PolymarketPositionUMConverter()

    @Test
    fun `GIVEN remaining loss and earlier realized profit WHEN converted THEN shows only remaining pnl`() {
        // Arrange
        val position = createPolymarketPosition()
        val expected = PolymarketPositionUM(
            id = "condition:asset",
            title = stringReference("Will the event happen?"),
            iconUrl = null,
            outcomeLabel = stringReference("Yes"),
            outcomeVariant = TangemMessageBubble.Variant.Info,
            priceChange = TangemPriceChange.State(
                value = stringReference("$0.03 (0.28%)"),
                direction = TangemPriceChange.Direction.Down,
            ),
            currentValue = stringReference("$9.97"),
            result = null,
        )

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN arbitrary outcome WHEN converted THEN preserves text and neutral colour`() {
        // Arrange
        val position = createPolymarketPosition(outcome = "Draw")

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual.outcomeLabel).isEqualTo(stringReference("Draw"))
        assertThat(actual.outcomeVariant).isEqualTo(TangemMessageBubble.Variant.Neutral)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN explicit settlement WHEN converted THEN result does not follow pnl sign`(model: ResultModel) {
        // Arrange
        val position = createPolymarketPosition(result = model.result)

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual.result).isEqualTo(
            TangemBadgeUM(text = stringReference(model.text), status = model.status, size = TangemBadge.Size.X6),
        )
    }

    private fun provideTestModels() = listOf(
        ResultModel(PolymarketPositionResult.WON, "Won $9.97", TangemBadge.Status.Success),
        ResultModel(PolymarketPositionResult.LOST, "Lost", TangemBadge.Status.Error),
        ResultModel(PolymarketPositionResult.SPLIT, "Split", TangemBadge.Status.Neutral),
        ResultModel(PolymarketPositionResult.UNKNOWN, "Unknown", TangemBadge.Status.Neutral),
    )

    data class ResultModel(val result: PolymarketPositionResult, val text: String, val status: TangemBadge.Status)
}