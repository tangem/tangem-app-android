package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.test.core.ProvideTestModels
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackSummaryDMConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cacheable summary WHEN converted to DM and back THEN summary is preserved`(model: RoundTripModel) {
        // Act
        val restored = CashbackSummaryDMConverter.convert(model.summary)?.let(CashbackSummaryDMConverter::convertBack)

        // Assert
        assertThat(restored).isEqualTo(model.summary)
    }

    @Test
    fun `GIVEN disabled summary WHEN convert THEN nothing to cache`() {
        // Act
        val actual = CashbackSummaryDMConverter.convert(CashbackSummary.Disabled)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN unknown summary WHEN convert THEN nothing to cache`() {
        // Act
        val actual = CashbackSummaryDMConverter.convert(CashbackSummary.Unknown)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        RoundTripModel(description = "full with all fields", summary = enabled(CashbackDisplayMode.FULL)),
        RoundTripModel(
            description = "alt_block without optional fields",
            summary = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.ALT_BLOCK,
                cashback = TangemPayCashback(
                    confirmedAmount = BigDecimal("-2.15"),
                    totalEarnedAmount = null,
                    currency = "USD",
                    payoutCurrency = null,
                    period = TangemPayCashback.Period(year = 2026, month = 1, payoutStart = null, payoutEnd = null),
                    previousPayout = null,
                ),
            ),
        ),
        RoundTripModel(description = "deactivated", summary = CashbackSummary.Deactivated),
    )

    private fun enabled(displayMode: CashbackDisplayMode) = CashbackSummary.Enabled(
        displayMode = displayMode,
        cashback = TangemPayCashback(
            confirmedAmount = BigDecimal("32.15"),
            totalEarnedAmount = BigDecimal("132.15"),
            currency = "USD",
            payoutCurrency = "USDC",
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = DateTime.parse("2026-07-02"),
                payoutEnd = DateTime.parse("2026-07-05"),
            ),
            previousPayout = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-06-05"),
                amount = BigDecimal("18.00"),
            ),
        ),
    )

    internal class RoundTripModel(val summary: CashbackSummary, private val description: String) {
        override fun toString(): String = description
    }
}