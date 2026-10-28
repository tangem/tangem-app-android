package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.ds2.util.TangemPriceChange.Direction.Down
import com.tangem.core.ui.ds2.util.TangemPriceChange.Direction.Neutral
import com.tangem.core.ui.ds2.util.TangemPriceChange.Direction.Up
import com.tangem.core.ui.extensions.stringReference
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPositionPnlTest {

    private val converter = PolymarketPositionUMConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a cash pnl WHEN card is converted THEN only amounts of at least one cent have direction`(case: PnlCase) {
        // Arrange
        val position = createPolymarketPosition(
            cashPnl = case.cashPnl.toBigDecimal(),
            percentPnl = case.percentPnl.toBigDecimal(),
        )

        // Act
        val actual = converter.convert(position).priceChange

        // Assert
        assertThat(actual).isEqualTo(
            TangemPriceChange.State(value = stringReference(case.text), direction = case.direction),
        )
    }

    private fun provideTestModels() = listOf(
        PnlCase("0", "$0.00 (12.34%)", Neutral),
        PnlCase("-0.0000", "$0.00 (12.34%)", Neutral),
        PnlCase("0.00000001", "<$0.01 (12.34%)", Neutral),
        PnlCase("-0.00000001", "<$0.01 (12.34%)", Neutral),
        PnlCase("0.0049", "<$0.01 (12.34%)", Neutral),
        PnlCase("-0.0049", "<$0.01 (12.34%)", Neutral),
        PnlCase("0.005", "<$0.01 (12.34%)", Neutral),
        PnlCase("-0.005", "<$0.01 (12.34%)", Neutral),
        PnlCase("0.0099", "<$0.01 (12.34%)", Neutral),
        PnlCase("-0.0099", "<$0.01 (12.34%)", Neutral),
        PnlCase("0.009999999999999999", "<$0.01 (12.34%)", Neutral),
        PnlCase("-0.009999999999999999", "<$0.01 (12.34%)", Neutral),
        PnlCase("0.01", "$0.01 (12.34%)", Up),
        PnlCase("-0.01", "$0.01 (12.34%)", Down),
        PnlCase("0.010000000000000001", "$0.01 (12.34%)", Up),
        PnlCase("-0.010000000000000001", "$0.01 (12.34%)", Down),
        PnlCase("0.02", "$0.02 (12.34%)", Up),
        PnlCase("-0.02", "$0.02 (12.34%)", Down),
        PnlCase("0.005", "<$0.01 (99.99%)", Neutral, percentPnl = "99.99"),
        PnlCase("-0.005", "<$0.01 (99.99%)", Neutral, percentPnl = "-99.99"),
        PnlCase("0.01", "$0.01 (0%)", Up, percentPnl = "0"),
        PnlCase("-0.01", "$0.01 (0%)", Down, percentPnl = "0"),
    )

    data class PnlCase(
        val cashPnl: String,
        val text: String,
        val direction: TangemPriceChange.Direction,
        val percentPnl: String = "12.34",
    )
}