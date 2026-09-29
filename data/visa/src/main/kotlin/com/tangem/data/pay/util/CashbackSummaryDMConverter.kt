package com.tangem.data.pay.util

import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.spend.datasource.pay.store.entity.CashbackSummaryDM

internal object CashbackSummaryDMConverter {

    /** `null` for a summary that must not be cached: the cache shows only what a fresh answer would show. */
    fun convert(value: CashbackSummary): CashbackSummaryDM? = when (value) {
        is CashbackSummary.Enabled -> value.toDM()
        CashbackSummary.Deactivated -> CashbackSummaryDM.Deactivated
        CashbackSummary.Disabled,
        CashbackSummary.Unknown,
        -> null
    }

    fun convertBack(value: CashbackSummaryDM): CashbackSummary = when (value) {
        is CashbackSummaryDM.Enabled -> value.toDomain()
        CashbackSummaryDM.Deactivated -> CashbackSummary.Deactivated
    }

    private fun CashbackSummary.Enabled.toDM() = CashbackSummaryDM.Enabled(
        displayMode = when (displayMode) {
            CashbackDisplayMode.FULL -> CashbackSummaryDM.DisplayMode.FULL
            CashbackDisplayMode.ALT_BLOCK -> CashbackSummaryDM.DisplayMode.ALT_BLOCK
        },
        confirmedAmount = cashback.confirmedAmount,
        totalEarnedAmount = cashback.totalEarnedAmount,
        currency = cashback.currency,
        payoutCurrency = cashback.payoutCurrency,
        period = CashbackSummaryDM.Period(
            year = cashback.period.year,
            month = cashback.period.month,
            payoutStart = cashback.period.payoutStart,
            payoutEnd = cashback.period.payoutEnd,
        ),
        previousPayout = cashback.previousPayout?.let { payout ->
            CashbackSummaryDM.PreviousPayout(endDate = payout.endDate, amount = payout.amount)
        },
    )

    private fun CashbackSummaryDM.Enabled.toDomain() = CashbackSummary.Enabled(
        displayMode = when (displayMode) {
            CashbackSummaryDM.DisplayMode.FULL -> CashbackDisplayMode.FULL
            CashbackSummaryDM.DisplayMode.ALT_BLOCK -> CashbackDisplayMode.ALT_BLOCK
        },
        cashback = TangemPayCashback(
            confirmedAmount = confirmedAmount,
            totalEarnedAmount = totalEarnedAmount,
            currency = currency,
            payoutCurrency = payoutCurrency,
            period = TangemPayCashback.Period(
                year = period.year,
                month = period.month,
                payoutStart = period.payoutStart,
                payoutEnd = period.payoutEnd,
            ),
            previousPayout = previousPayout?.let { payout ->
                TangemPayCashback.PreviousPayout(endDate = payout.endDate, amount = payout.amount)
            },
        ),
    )
}