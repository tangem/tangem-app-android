package com.tangem.data.pay.util

import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackProgramStatus
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.spend.datasource.pay.models.response.CashbackSummaryResponse
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object CashbackSummaryConverter : Converter<CashbackSummaryResponse, CashbackSummary> {

    private const val FIAT_CURRENCY_CODE = "USD"

    override fun convert(value: CashbackSummaryResponse): CashbackSummary {
        val result = value.result ?: return CashbackSummary.Unknown
        return when (CashbackProgramStatus.fromString(result.cashbackProgramStatus)) {
            CashbackProgramStatus.ENABLED -> toEnabled(result)
            CashbackProgramStatus.DEACTIVATED -> CashbackSummary.Deactivated
            CashbackProgramStatus.DISABLED -> CashbackSummary.Disabled
            CashbackProgramStatus.UNKNOWN -> CashbackSummary.Unknown
        }
    }

    private fun toEnabled(result: CashbackSummaryResponse.Result): CashbackSummary {
        val period = result.period
        return CashbackSummary.Enabled(
            displayMode = CashbackDisplayMode.fromString(result.cashbackDisplayMode),
            cashback = TangemPayCashback(
                confirmedAmount = result.confirmedAmount,
                totalEarnedAmount = result.totalEarnedAmount,
                currency = result.currency.takeIf(String::isNotBlank) ?: FIAT_CURRENCY_CODE,
                payoutCurrency = result.payoutCurrency?.takeIf(String::isNotBlank),
                period = TangemPayCashback.Period(
                    year = period.year,
                    month = period.month,
                    payoutStart = period.payoutStartDate?.parseDate(),
                    payoutEnd = period.payoutEndDate?.parseDate(),
                ),
                previousPayout = result.toPreviousPayout(),
            ),
        )
    }

    private fun CashbackSummaryResponse.Result.toPreviousPayout(): TangemPayCashback.PreviousPayout? {
        val endDate = previousPayoutEndDate?.parseDate() ?: return null
        val amount = previousPayoutAmount ?: return null
        return TangemPayCashback.PreviousPayout(endDate = endDate, amount = amount)
    }

    private fun String.parseDate(): DateTime? = runCatching { DateTime.parse(this) }.getOrNull()
}