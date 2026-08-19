package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.spend.datasource.pay.models.response.CashbackSummaryResponse
import org.joda.time.DateTime
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackSummaryConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = CashbackSummaryConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "enabled + full -> Enabled(FULL)",
            response = createResponse(status = "enabled", displayMode = "full"),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(),
            ),
        ),
        ConvertModel(
            name = "enabled + alt_block -> Enabled(ALT_BLOCK)",
            response = createResponse(status = "enabled", displayMode = "alt_block"),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.ALT_BLOCK,
                cashback = expectedCashback(),
            ),
        ),
        ConvertModel(
            name = "enabled + no previous payout amount -> previousPayout is null",
            response = createResponse(previousPayoutAmount = null),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(previousPayout = null),
            ),
        ),
        ConvertModel(
            name = "enabled + no previous payout date -> previousPayout is null",
            response = createResponse(previousPayoutEndDate = null),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(previousPayout = null),
            ),
        ),
        ConvertModel(
            name = "enabled + malformed previous payout date -> previousPayout is null",
            response = createResponse(previousPayoutEndDate = "not-a-date"),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(previousPayout = null),
            ),
        ),
        ConvertModel(
            name = "enabled + no payout window -> Enabled without payout dates",
            response = createResponse(
                period = CashbackSummaryResponse.Period(
                    year = 2026,
                    month = 6,
                    payoutStartDate = null,
                    payoutEndDate = null,
                ),
            ),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(payoutStart = null, payoutEnd = null),
            ),
        ),
        ConvertModel(
            name = "enabled + malformed payout start -> Enabled without that date",
            response = createResponse(
                period = CashbackSummaryResponse.Period(
                    year = 2026,
                    month = 6,
                    payoutStartDate = "07/02/2026",
                    payoutEndDate = "2026-07-05",
                ),
            ),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(payoutStart = null),
            ),
        ),
        ConvertModel(
            name = "fraud -> Deactivated",
            response = createResponse(status = "fraud"),
            expected = CashbackSummary.Deactivated,
        ),
        ConvertModel(
            name = "deactivated -> Deactivated",
            response = createResponse(status = "deactivated"),
            expected = CashbackSummary.Deactivated,
        ),
        ConvertModel(
            name = "disabled -> Disabled",
            response = createResponse(status = "disabled"),
            expected = CashbackSummary.Disabled,
        ),
        ConvertModel(
            name = "unavailable -> Disabled",
            response = createResponse(status = "unavailable"),
            expected = CashbackSummary.Disabled,
        ),
        ConvertModel(
            name = "unrecognized status -> Unknown",
            response = createResponse(status = "something_new"),
            expected = CashbackSummary.Unknown,
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val response: CashbackSummaryResponse,
        val expected: CashbackSummary,
    ) {
        override fun toString(): String = name
    }

    private companion object {

        fun createResponse(
            status: String = "enabled",
            displayMode: String = "full",
            period: CashbackSummaryResponse.Period = CashbackSummaryResponse.Period(
                year = 2026,
                month = 6,
                payoutStartDate = "2026-07-02",
                payoutEndDate = "2026-07-05",
            ),
            confirmedAmount: BigDecimal = BigDecimal("22.54"),
            totalEarnedAmount: BigDecimal? = BigDecimal("132.15"),
            previousPayoutEndDate: String? = "2026-06-05",
            previousPayoutAmount: BigDecimal? = BigDecimal("18.00"),
            currency: String = "USD",
        ) = CashbackSummaryResponse(
            result = CashbackSummaryResponse.Result(
                cashbackProgramStatus = status,
                cashbackDisplayMode = displayMode,
                period = period,
                confirmedAmount = confirmedAmount,
                totalEarnedAmount = totalEarnedAmount,
                previousPayoutEndDate = previousPayoutEndDate,
                previousPayoutAmount = previousPayoutAmount,
                currency = currency,
            ),
        )

        fun expectedCashback(
            confirmedAmount: BigDecimal = BigDecimal("22.54"),
            totalEarnedAmount: BigDecimal? = BigDecimal("132.15"),
            previousPayout: TangemPayCashback.PreviousPayout? = TangemPayCashback.PreviousPayout(
                endDate = DateTime.parse("2026-06-05"),
                amount = BigDecimal("18.00"),
            ),
            payoutStart: DateTime? = DateTime.parse("2026-07-02"),
            payoutEnd: DateTime? = DateTime.parse("2026-07-05"),
        ) = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            totalEarnedAmount = totalEarnedAmount,
            currency = "USD",
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = payoutStart,
                payoutEnd = payoutEnd,
            ),
            previousPayout = previousPayout,
        )
    }
}