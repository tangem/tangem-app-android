package com.tangem.features.polymarket.impl.positionresult.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketPositionResultConverterTest {

    private val converter = PolymarketPositionResultConverter()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN resolved holding WHEN converted THEN shows its result and remaining amounts`(case: ResultCase) {
        // Arrange
        val position = createPolymarketPosition(
            result = case.result,
            resolutionStatus = PolymarketResolutionStatus.RESOLVED,
            cashPnl = BigDecimal("20"),
            percentPnl = BigDecimal("20"),
            realizedPnl = BigDecimal("999"),
        ).copy(
            initialValue = BigDecimal("100"),
            currentValue = BigDecimal(case.currentValue),
            isRedeemable = case.result != PolymarketPositionResult.LOST,
            iconUrl = "https://example.com/market.png",
        )
        val expected = PolymarketPositionResultUM(
            market = MarketHeaderUM(
                title = "Will the event happen?",
                imageUrl = "https://example.com/market.png",
                outcomeTitle = "Yes",
                outcomePriceCents = 36,
                resolutionDateMillis = null,
            ),
            result = case.uiResult,
            title = stringReference(case.title),
            resolvedAt = null,
            invested = if (case.result == PolymarketPositionResult.LOST) null else PolymarketPositionResultUM.Metric(
                title = resourceReference(R.string.prediction_positions_invested),
                value = stringReference("$100.00"),
            ),
            profit = if (case.showProfit) PolymarketPositionResultUM.Metric(
                title = stringReference("Profit"),
                value = stringReference("▲ $20.00 (20%)"),
            ) else null,
            footerNote = if (case.result == PolymarketPositionResult.LOST) null else {
                resourceReference(R.string.prediction_position_result_balance_note)
            },
        )

        // Act
        val actual = converter.convert(position)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PolymarketResolutionStatus::class, names = ["RESOLVED"], mode = EnumSource.Mode.EXCLUDE)
    fun `GIVEN unresolved market WHEN checked THEN result screen is unavailable`(status: PolymarketResolutionStatus) {
        // Arrange
        val position = createPolymarketPosition(result = PolymarketPositionResult.WON, resolutionStatus = status)
            .copy(isRedeemable = true)

        // Act
        val actual = position.canOpenPositionResult

        // Assert
        assertThat(actual).isFalse()
    }

    private fun provideTestModels() = listOf(
        ResultCase(PolymarketPositionResult.WON, "120", PolymarketPositionResultUM.Result.Won, "You Won $120.00", true),
        ResultCase(PolymarketPositionResult.LOST, "0", PolymarketPositionResultUM.Result.Lost, "Loss $100.00", false),
        ResultCase(PolymarketPositionResult.SPLIT, "120", PolymarketPositionResultUM.Result.Split, "Split $120.00", true),
        ResultCase(PolymarketPositionResult.SPLIT, "100", PolymarketPositionResultUM.Result.Split, "Split $100.00", false),
        ResultCase(PolymarketPositionResult.SPLIT, "80", PolymarketPositionResultUM.Result.Split, "Split $80.00", false),
    )

    @ParameterizedTest
    @EnumSource(PolymarketPositionResult::class)
    fun `GIVEN resolved position without claim WHEN checked THEN only loss can be displayed`(result: PolymarketPositionResult) {
        // Arrange
        val position = createPolymarketPosition(result = result, resolutionStatus = PolymarketResolutionStatus.RESOLVED)

        // Act
        val actual = position.canOpenPositionResult

        // Assert
        assertThat(actual).isEqualTo(result == PolymarketPositionResult.LOST)
    }

    data class ResultCase(
        val result: PolymarketPositionResult,
        val currentValue: String,
        val uiResult: PolymarketPositionResultUM.Result,
        val title: String,
        val showProfit: Boolean,
    )
}