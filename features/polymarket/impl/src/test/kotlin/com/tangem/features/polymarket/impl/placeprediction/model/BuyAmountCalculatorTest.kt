package com.tangem.features.polymarket.impl.placeprediction.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.features.polymarket.impl.placeprediction.entity.BuyAmountFractionUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BuyAmountCalculatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a balance WHEN fraction THEN its share in whole cents, Max leaving room for the fees`(
        model: FractionModel,
    ) {
        // Arrange
        val lastQuote = model.feeTotal?.let { createQuote(feeTotal = BigDecimal(it), notional = BigDecimal("10")) }

        // Act
        val actual = BuyAmountCalculator.fraction(
            balance = BigDecimal(model.balance),
            fraction = model.fraction,
            lastQuote = lastQuote,
        )

        // Assert
        assertThat(actual).isEqualTo(BigDecimal(model.expected))
    }

    private fun provideTestModels(): List<FractionModel> = listOf(
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Quarter, expected = "25.00"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Half, expected = "50.00"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.ThreeQuarters, expected = "75.00"),
        FractionModel(balance = "33.33", fraction = BuyAmountFractionUM.Half, expected = "16.66"),
        FractionModel(balance = "0.01", fraction = BuyAmountFractionUM.Quarter, expected = "0.00"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Max, expected = "90.00"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Max, feeTotal = "0.4", expected = "90.00"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Max, feeTotal = "2", expected = "83.33"),
        FractionModel(balance = "100", fraction = BuyAmountFractionUM.Quarter, feeTotal = "2", expected = "25.00"),
    )

    private fun createQuote(feeTotal: BigDecimal, notional: BigDecimal): PredictionOrderQuote = mockk {
        every { this@mockk.notional } returns notional
        every { fees } returns PredictionOrderFees(market = feeTotal, builder = BigDecimal.ZERO, total = feeTotal)
    }

    data class FractionModel(
        val balance: String,
        val fraction: BuyAmountFractionUM,
        val expected: String,
        val feeTotal: String? = null,
    )
}