package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketClosedPositionStatus
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketClosedPositionUMConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN closed outcome WHEN converted THEN shows realized pnl without invented percentage or payout`(
        model: ResultModel,
    ) {
        // Arrange
        val position = createClosedPolymarketPosition(status = model.result)
        val expected = PolymarketPositionUM(
            id = "condition:asset",
            title = stringReference("Will the event happen?"),
            iconUrl = null,
            outcomeLabel = stringReference("Yes"),
            outcomeVariant = TangemMessageBubble.Variant.Info,
            priceChange = TangemPriceChange.State(stringReference("$12.40"), TangemPriceChange.Direction.Up),
            currentValue = null,
            result = TangemBadgeUM(resourceReference(model.label), status = model.status, size = TangemBadge.Size.X6),
        )

        // Act
        val actual = PolymarketPositionUMConverter().convert(position)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        ResultModel(PolymarketClosedPositionStatus.CLOSED, R.string.prediction_positions_closed, TangemBadge.Status.Neutral),
        ResultModel(PolymarketClosedPositionStatus.WON, R.string.prediction_positions_won, TangemBadge.Status.Success),
        ResultModel(PolymarketClosedPositionStatus.LOST, R.string.prediction_positions_lost, TangemBadge.Status.Error),
        ResultModel(PolymarketClosedPositionStatus.SPLIT, R.string.prediction_positions_split, TangemBadge.Status.Neutral),
        ResultModel(PolymarketClosedPositionStatus.UNKNOWN, R.string.prediction_positions_unknown, TangemBadge.Status.Neutral),
    )

    data class ResultModel(val result: PolymarketClosedPositionStatus, val label: Int, val status: TangemBadge.Status)
}