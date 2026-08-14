package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CashbackSummaryResponse
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackProgramStatus
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime
import java.math.BigDecimal

/** Maps [CashbackSummaryResponse] (BFF) to the domain [CashbackSummary]. */
internal object CashbackSummaryConverter : Converter<CashbackSummaryResponse, CashbackSummary> {

    override fun convert(value: CashbackSummaryResponse): CashbackSummary {
        return when (CashbackProgramStatus.fromString(value.cashbackProgramStatus)) {
            CashbackProgramStatus.ENABLED -> toEnabled(value)
            CashbackProgramStatus.DEACTIVATED -> CashbackSummary.Deactivated
            CashbackProgramStatus.DISABLED -> CashbackSummary.Disabled
            CashbackProgramStatus.UNKNOWN -> CashbackSummary.Unknown
        }
    }

    private fun toEnabled(value: CashbackSummaryResponse): CashbackSummary {
        val period = value.period ?: return CashbackSummary.Unknown
        val payoutStart = period.payoutStartDate?.let(DateTime::parse) ?: return CashbackSummary.Unknown
        val payoutEnd = period.payoutEndDate?.let(DateTime::parse) ?: return CashbackSummary.Unknown

        return CashbackSummary.Enabled(
            displayMode = CashbackDisplayMode.fromString(value.cashbackDisplayMode),
            cashback = TangemPayCashback(
                confirmedAmount = value.confirmedAmount ?: BigDecimal.ZERO,
                pendingAmount = value.pendingAmount ?: BigDecimal.ZERO,
                currency = value.currency.orEmpty(),
                payoutCurrency = value.payoutCurrency.orEmpty(),
                payoutNetwork = value.payoutNetwork.orEmpty(),
                period = TangemPayCashback.Period(
                    year = period.year,
                    month = period.month,
                    payoutStart = payoutStart,
                    payoutEnd = payoutEnd,
                ),
            ),
        )
    }
}