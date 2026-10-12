package com.tangem.features.polymarket.impl.positionresult.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.placeprediction.entity.MarketHeaderUM
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
            isRedeemable = case.isRedeemable,
            iconUrl = "https://example.com/market.png",
        )
        val expected = PolymarketPositionResultUM(
            market = MarketHeaderUM(
                title = "Will the event happen?",
                imageUrl = "https://example.com/market.png",
                outcomeTitle = "Yes",
                outcomeAccent = PolymarketOutcomeAccent.Info,
                outcomePriceCents = 36,
                resolutionDateMillis = null,
            ),
            result = case.uiResult,
            title = resourceReference(case.titleRes, wrappedList(case.titleAmount)),
            resolvedAt = stringReference(DATE_FORMATTER.format(END_DATE)),
            invested = if (case.result == PolymarketPositionResult.LOST) null else PolymarketPositionResultUM.Metric(
                title = resourceReference(R.string.prediction_positions_invested),
                value = stringReference("$100.00"),
            ),
            profit = if (case.showProfit) {
                PolymarketPositionResultUM.Metric(
                    title = resourceReference(R.string.prediction_position_result_profit),
                    value = stringReference("▲ $20.00 (20%)"),
                )
            } else {
                null
            },
            footerNote = case.footerRes?.let { resourceReference(it) },
            action = case.action,
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

    @ParameterizedTest
    @EnumSource(PolymarketPositionResult::class)
    fun `GIVEN a resolved position whose payout is not on chain WHEN checked THEN only an unknown result is hidden`(
        result: PolymarketPositionResult,
    ) {
        // Arrange
        val position = createPolymarketPosition(result = result, resolutionStatus = PolymarketResolutionStatus.RESOLVED)

        // Act
        val actual = position.canOpenPositionResult

        // Assert
        assertThat(actual).isEqualTo(result != PolymarketPositionResult.UNKNOWN)
    }

    private fun provideTestModels() = listOf(
        ResultCase(
            result = PolymarketPositionResult.WON,
            currentValue = "120",
            isRedeemable = true,
            uiResult = PolymarketPositionResultUM.Result.Won,
            titleRes = R.string.prediction_position_result_won_title,
            titleAmount = "$120.00",
            showProfit = true,
            footerRes = R.string.prediction_position_result_balance_note,
            action = PolymarketPositionResultUM.Action.Claim(
                isInProgress = false,
                label = resourceReference(R.string.prediction_position_result_claim_action),
            ),
        ),
        ResultCase(
            result = PolymarketPositionResult.WON,
            currentValue = "120",
            isRedeemable = false,
            uiResult = PolymarketPositionResultUM.Result.Won,
            titleRes = R.string.prediction_position_result_won_title,
            titleAmount = "$120.00",
            showProfit = true,
            footerRes = R.string.prediction_position_result_payout_pending_note,
            action = PolymarketPositionResultUM.Action.Dismiss,
        ),
        ResultCase(
            result = PolymarketPositionResult.LOST,
            currentValue = "0",
            isRedeemable = true,
            uiResult = PolymarketPositionResultUM.Result.Lost,
            titleRes = R.string.prediction_position_result_loss_title,
            titleAmount = "$100.00",
            showProfit = false,
            footerRes = R.string.prediction_position_result_close_note,
            action = PolymarketPositionResultUM.Action.Claim(
                isInProgress = false,
                label = resourceReference(R.string.prediction_position_result_close_action),
            ),
        ),
        ResultCase(
            result = PolymarketPositionResult.LOST,
            currentValue = "0",
            isRedeemable = false,
            uiResult = PolymarketPositionResultUM.Result.Lost,
            titleRes = R.string.prediction_position_result_loss_title,
            titleAmount = "$100.00",
            showProfit = false,
            footerRes = null,
            action = PolymarketPositionResultUM.Action.Dismiss,
        ),
        ResultCase(
            result = PolymarketPositionResult.SPLIT,
            currentValue = "120",
            isRedeemable = true,
            uiResult = PolymarketPositionResultUM.Result.Split,
            titleRes = R.string.prediction_position_result_split_title,
            titleAmount = "$120.00",
            showProfit = true,
            footerRes = R.string.prediction_position_result_balance_note,
            action = PolymarketPositionResultUM.Action.Claim(
                isInProgress = false,
                label = resourceReference(R.string.prediction_position_result_claim_action),
            ),
        ),
        ResultCase(
            result = PolymarketPositionResult.SPLIT,
            currentValue = "100",
            isRedeemable = false,
            uiResult = PolymarketPositionResultUM.Result.Split,
            titleRes = R.string.prediction_position_result_split_title,
            titleAmount = "$100.00",
            showProfit = false,
            footerRes = R.string.prediction_position_result_payout_pending_note,
            action = PolymarketPositionResultUM.Action.Dismiss,
        ),
        ResultCase(
            result = PolymarketPositionResult.SPLIT,
            currentValue = "80",
            isRedeemable = true,
            uiResult = PolymarketPositionResultUM.Result.Split,
            titleRes = R.string.prediction_position_result_split_title,
            titleAmount = "$80.00",
            showProfit = false,
            footerRes = R.string.prediction_position_result_balance_note,
            action = PolymarketPositionResultUM.Action.Claim(
                isInProgress = false,
                label = resourceReference(R.string.prediction_position_result_claim_action),
            ),
        ),
    )

    data class ResultCase(
        val result: PolymarketPositionResult,
        val currentValue: String,
        val isRedeemable: Boolean,
        val uiResult: PolymarketPositionResultUM.Result,
        val titleRes: Int,
        val titleAmount: String,
        val showProfit: Boolean,
        val footerRes: Int?,
        val action: PolymarketPositionResultUM.Action,
    )

    private companion object {
        val END_DATE: LocalDate = LocalDate.of(2027, 4, 30)
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
    }
}