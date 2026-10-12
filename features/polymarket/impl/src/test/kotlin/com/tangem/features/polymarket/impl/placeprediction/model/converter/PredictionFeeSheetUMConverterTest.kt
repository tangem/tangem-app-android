package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionFeeSheetUMConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a quote WHEN converted THEN breaks the total down by the order side`(case: ConvertCase) {
        // Act
        val actual = PredictionFeeSheetUMConverter.convert(
            quote = case.quote,
            slippage = case.slippage,
            tokenSymbol = case.tokenSymbol,
        )

        // Assert
        assertThat(actual).isEqualTo(case.expected)
    }

    private fun provideTestModels() = listOf(
        ConvertCase(
            quote = createQuote(),
            slippage = SlippageUM.of(BigDecimal("0.25")),
            tokenSymbol = "USDC",
            expected = createFeeSheet(
                amounts = listOf("$12.00", "$0.00", "$0.30", "$12.30"),
                descriptionRes = R.string.prediction_fee_sheet_buy_description,
                descriptionArgs = wrappedList("20", "60", "USDC", "0.25%"),
            ),
        ),
        ConvertCase(
            quote = createQuote().copy(
                side = PredictionOrderSide.SELL,
                shares = BigDecimal("20"),
                expectedExecutionAmount = BigDecimal("12"),
                notional = BigDecimal("11.80"),
                fees = PredictionOrderFees(
                    market = BigDecimal("0.20"),
                    builder = BigDecimal("0.30"),
                    total = BigDecimal("0.50"),
                ),
                total = BigDecimal("11.30"),
            ),
            slippage = SlippageUM.of(BigDecimal("2")),
            tokenSymbol = "pUSD",
            expected = createFeeSheet(
                amounts = listOf("$11.80", "$0.20", "$0.30", "$11.30"),
                descriptionRes = R.string.prediction_fee_sheet_sell_description,
                descriptionArgs = wrappedList("20", "60", "pUSD", "2%"),
            ),
        ),
    )

    private fun createFeeSheet(
        amounts: List<String>,
        descriptionRes: Int,
        descriptionArgs: WrappedList<Any>,
    ) = PredictionFeeSheetUM(
        rows = persistentListOf(
            PredictionFeeSheetUM.Row(
                title = resourceReference(R.string.prediction_fee_sheet_order),
                amount = amounts[0],
                description = resourceReference(descriptionRes, descriptionArgs),
            ),
            PredictionFeeSheetUM.Row(
                title = resourceReference(R.string.prediction_fee_sheet_polymarket_fee),
                amount = amounts[1],
            ),
            PredictionFeeSheetUM.Row(
                title = resourceReference(R.string.prediction_fee_sheet_builder_fee),
                amount = amounts[2],
            ),
            PredictionFeeSheetUM.Row(
                title = resourceReference(R.string.prediction_fee_sheet_total),
                amount = amounts[3],
            ),
        ),
    )

    private fun createQuote() = PredictionOrderQuote(
        status = PredictionQuoteStatus.FULL,
        side = PredictionOrderSide.BUY,
        shares = BigDecimal("19.67"),
        notional = BigDecimal("12"),
        expectedExecutionAmount = BigDecimal("20"),
        averagePrice = BigDecimal("0.60"),
        worstCasePrice = BigDecimal("0.61"),
        fees = PredictionOrderFees(
            market = BigDecimal.ZERO,
            builder = BigDecimal("0.30"),
            total = BigDecimal("0.30"),
        ),
        total = BigDecimal("12.30"),
        builderCode = "builder",
        minOrderSize = BigDecimal("5"),
        minOrderAmount = BigDecimal.ONE,
        tickSize = BigDecimal("0.01"),
        isLive = false,
    )

    data class ConvertCase(
        val quote: PredictionOrderQuote,
        val slippage: SlippageUM,
        val tokenSymbol: String,
        val expected: PredictionFeeSheetUM,
    )
}