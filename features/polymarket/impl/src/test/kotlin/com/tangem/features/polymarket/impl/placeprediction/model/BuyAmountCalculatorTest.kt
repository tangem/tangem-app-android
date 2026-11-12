package com.tangem.features.polymarket.impl.placeprediction.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BuyAmountCalculatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a balance WHEN fraction THEN its share in whole cents, Max leaving room for the fees`(
        model: FractionModel,
    ) {
        // Act
        val actual = BuyAmountCalculator.fraction(
            balance = BigDecimal(model.balance),
            fraction = model.fraction,
            feeRate = BigDecimal(model.feeRate),
        )

        // Assert
        assertThat(actual).isEqualTo(BigDecimal(model.expected))
    }

    @ParameterizedTest
    @MethodSource("provideFeeRateModels")
    fun `GIVEN a quote WHEN feeRate THEN fees over notional, none for an empty quote`(model: FeeRateModel) {
        // Arrange
        val quote = mockk<PredictionOrderQuote> {
            every { notional } returns BigDecimal(model.notional)
            every { fees } returns PredictionOrderFees(
                market = BigDecimal(model.fees),
                builder = BigDecimal.ZERO,
                total = BigDecimal(model.fees),
            )
        }

        // Act
        val actual = BuyAmountCalculator.feeRate(quote)

        // Assert
        assertThat(actual).isEqualTo(model.expected?.let(::BigDecimal))
    }

    @ParameterizedTest
    @MethodSource("provideQuoteUMFeeRateModels")
    fun `GIVEN the current quote WHEN feeRate THEN fees over notional of a priced quote only, else none`(
        model: QuoteUMFeeRateModel,
    ) {
        // Act
        val actual = BuyAmountCalculator.feeRate(quote = model.quote)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal(model.expected))
    }

    private fun provideQuoteUMFeeRateModels(): List<QuoteUMFeeRateModel> = listOf(
        QuoteUMFeeRateModel(quote = pricedQuote(fees = "0.4", notional = "10"), expected = "0.04"),
        QuoteUMFeeRateModel(quote = pricedQuote(fees = "0", notional = "0"), expected = "0"),
        QuoteUMFeeRateModel(quote = QuoteUM.Empty, expected = "0"),
        QuoteUMFeeRateModel(quote = QuoteUM.Loading, expected = "0"),
        QuoteUMFeeRateModel(quote = QuoteUM.Error(reason = QuoteErrorUM.Network), expected = "0"),
        QuoteUMFeeRateModel(
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
            expected = "0",
        ),
    )

    private fun pricedQuote(fees: String, notional: String): QuoteUM.Content {
        val quote = mockk<PredictionOrderQuote> {
            every { this@mockk.notional } returns BigDecimal(notional)
            every { this@mockk.fees } returns PredictionOrderFees(
                market = BigDecimal(fees),
                builder = BigDecimal.ZERO,
                total = BigDecimal(fees),
            )
        }
        return QuoteUM.Content(
            status = PredictionQuoteStatus.FULL,
            expected = BigDecimal.ONE,
            feeTotal = BigDecimal(fees),
            total = BigDecimal.ONE,
            signable = SignableQuoteUM(quote = quote, receivedAtMillis = 0L, assetId = "asset"),
        )
    }

    private fun provideFeeRateModels(): List<FeeRateModel> = listOf(
        FeeRateModel(fees = "0.4", notional = "10", expected = "0.04"),
        FeeRateModel(fees = "0", notional = "0", expected = null),
    )

    private fun provideTestModels(): List<FractionModel> = listOf(
        FractionModel(balance = "100", fraction = AmountFractionUM.Quarter, expected = "25.00"),
        FractionModel(balance = "100", fraction = AmountFractionUM.Half, expected = "50.00"),
        FractionModel(balance = "100", fraction = AmountFractionUM.ThreeQuarters, expected = "75.00"),
        FractionModel(balance = "33.33", fraction = AmountFractionUM.Half, expected = "16.66"),
        FractionModel(balance = "0.01", fraction = AmountFractionUM.Quarter, expected = "0.00"),
        FractionModel(balance = "100", fraction = AmountFractionUM.Max, expected = "90.00"),
        FractionModel(balance = "100", fraction = AmountFractionUM.Max, feeRate = "0.04", expected = "90.00"),
        FractionModel(balance = "100", fraction = AmountFractionUM.Max, feeRate = "0.2", expected = "83.33"),
        FractionModel(balance = "100", fraction = AmountFractionUM.Quarter, feeRate = "0.2", expected = "25.00"),
    )

    data class FractionModel(
        val balance: String,
        val fraction: AmountFractionUM,
        val expected: String,
        val feeRate: String = "0",
    )

    data class QuoteUMFeeRateModel(val quote: QuoteUM, val expected: String)

    data class FeeRateModel(val fees: String, val notional: String, val expected: String?)
}