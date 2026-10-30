package com.tangem.features.polymarket.impl.common

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
internal class PolymarketPnlTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a pnl amount WHEN presented THEN text sign and direction agree at collateral precision`(case: PnlCase) {
        // Arrange
        val amount = case.amount.toBigDecimal()

        // Act
        val actual = amount.toPolymarketPnl()

        // Assert
        assertThat(actual).isEqualTo(
            TangemPriceChange.State(value = stringReference(case.text), direction = case.direction),
        )
    }

    private fun provideTestModels() = listOf(
        PnlCase("0", "$0.00", Neutral),
        PnlCase("0.00", "$0.00", Neutral),
        PnlCase("-0.0000", "$0.00", Neutral),
        PnlCase("0.00000001", "$0.00", Neutral),
        PnlCase("-0.00000001", "$0.00", Neutral),
        PnlCase("0.004", "$0.00", Neutral),
        PnlCase("-0.004", "$0.00", Neutral),
        PnlCase("0.004999999999999999", "$0.00", Neutral),
        PnlCase("-0.004999999999999999", "$0.00", Neutral),
        PnlCase("0.005", "+$0.01", Up),
        PnlCase("-0.005", "-$0.01", Down),
        PnlCase("0.005000000000000001", "+$0.01", Up),
        PnlCase("-0.005000000000000001", "-$0.01", Down),
        PnlCase("0.0099", "+$0.01", Up),
        PnlCase("-0.0099", "-$0.01", Down),
        PnlCase("0.01", "+$0.01", Up),
        PnlCase("-0.01", "-$0.01", Down),
        PnlCase("0.02", "+$0.02", Up),
        PnlCase("-0.02", "-$0.02", Down),
        PnlCase("9.995", "+$10.00", Up),
        PnlCase("-9.995", "-$10.00", Down),
        PnlCase("10000000000000000.004999", "+$10,000,000,000,000,000.00", Up),
    )

    data class PnlCase(val amount: String, val text: String, val direction: TangemPriceChange.Direction)
}