package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_16
import com.tangem.core.ui.res.generated.icons.ic_heart_broken_16
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_16
import java.math.BigDecimal
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
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
        val position = createClosedPolymarketPosition(status = model.result, realizedPnl = model.pnl)
        val expected = PolymarketPositionUM(
            id = "condition:asset",
            title = stringReference("Will the event happen?"),
            iconUrl = null,
            outcomeLabel = stringReference("Yes"),
            outcomeVariant = TangemMessageBubble.Variant.Info,
            priceChange = null,
            currentValue = null,
            result = TangemBadgeUM(
                text = resourceReference(
                    R.string.prediction_positions_result_amount,
                    wrappedList(resourceReference(model.label), model.amount),
                ),
                status = model.status,
                size = TangemBadge.Size.X6,
                iconStart = model.icon,
            ),
        )

        // Act
        val actual = PolymarketPositionUMConverter().convert(position)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        ResultModel(PolymarketClosedPositionStatus.CLOSED, R.string.prediction_positions_closed, TangemBadge.Status.Success),
        ResultModel(
            PolymarketClosedPositionStatus.CLOSED, R.string.prediction_positions_closed, TangemBadge.Status.Error,
            pnl = BigDecimal("-12.40"),
        ),
        ResultModel(
            PolymarketClosedPositionStatus.CLOSED, R.string.prediction_positions_closed, TangemBadge.Status.Neutral,
            pnl = BigDecimal.ZERO, amount = "$0.00",
        ),
        ResultModel(
            PolymarketClosedPositionStatus.WON, R.string.prediction_positions_won, TangemBadge.Status.Success,
            icon = TangemIconUM.Icon(imageVector = Icons.ic_checkmark_16),
        ),
        ResultModel(
            PolymarketClosedPositionStatus.LOST, R.string.prediction_positions_lost, TangemBadge.Status.Error,
            icon = TangemIconUM.Icon(imageVector = Icons.ic_heart_broken_16),
        ),
        ResultModel(
            PolymarketClosedPositionStatus.SPLIT, R.string.prediction_positions_split, TangemBadge.Status.Neutral,
            icon = TangemIconUM.Icon(imageVector = Icons.ic_arrow_split_top_16),
        ),
        ResultModel(PolymarketClosedPositionStatus.UNKNOWN, R.string.prediction_positions_unknown, TangemBadge.Status.Neutral),
    )

    data class ResultModel(
        val result: PolymarketClosedPositionStatus,
        val label: Int,
        val status: TangemBadge.Status,
        val pnl: BigDecimal = BigDecimal("12.40"),
        val amount: String = "$12.40",
        val icon: TangemIconUM? = null,
    )
}