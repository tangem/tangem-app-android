package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CashbackSummaryResponse
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
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
            name = "enabled + missing display mode -> Enabled(FULL)",
            response = createResponse(status = "enabled", displayMode = null),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(),
            ),
        ),
        ConvertModel(
            name = "enabled + zero amounts -> Enabled with zeros",
            response = createResponse(
                status = "enabled",
                displayMode = "full",
                confirmedAmount = null,
                pendingAmount = null,
            ),
            expected = CashbackSummary.Enabled(
                displayMode = CashbackDisplayMode.FULL,
                cashback = expectedCashback(confirmedAmount = BigDecimal.ZERO, pendingAmount = BigDecimal.ZERO),
            ),
        ),
        ConvertModel(
            name = "enabled but missing period -> Unknown",
            response = createResponse(status = "enabled", period = null),
            expected = CashbackSummary.Unknown,
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
            displayMode: String? = "full",
            period: CashbackSummaryResponse.Period? = CashbackSummaryResponse.Period(
                year = 2026,
                month = 6,
                payoutStartDate = "2026-07-02",
                payoutEndDate = "2026-07-05",
            ),
            confirmedAmount: BigDecimal? = BigDecimal("22.54"),
            pendingAmount: BigDecimal? = BigDecimal("13.65"),
            currency: String? = "USD",
            payoutCurrency: String? = "USDC",
            payoutNetwork: String? = "Polygon",
        ) = CashbackSummaryResponse(
            cashbackProgramStatus = status,
            cashbackDisplayMode = displayMode,
            period = period,
            confirmedAmount = confirmedAmount,
            pendingAmount = pendingAmount,
            currency = currency,
            payoutCurrency = payoutCurrency,
            payoutNetwork = payoutNetwork,
        )

        fun expectedCashback(
            confirmedAmount: BigDecimal = BigDecimal("22.54"),
            pendingAmount: BigDecimal = BigDecimal("13.65"),
        ) = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            pendingAmount = pendingAmount,
            currency = "USD",
            payoutCurrency = "USDC",
            payoutNetwork = "Polygon",
            period = TangemPayCashback.Period(
                year = 2026,
                month = 6,
                payoutStart = DateTime.parse("2026-07-02"),
                payoutEnd = DateTime.parse("2026-07-05"),
            ),
        )
    }
}