package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CashbackHistoryResponse
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/** Maps [CashbackHistoryResponse] (BFF) to the domain [CashbackHistory]. */
internal object CashbackHistoryConverter : Converter<CashbackHistoryResponse, CashbackHistory> {

    override fun convert(value: CashbackHistoryResponse): CashbackHistory {
        return CashbackHistory(
            currency = value.currency.orEmpty(),
            months = value.items.orEmpty().map { item ->
                CashbackHistory.MonthlyCashback(
                    year = item.year,
                    month = item.month,
                    confirmedAmount = item.confirmedAmount ?: BigDecimal.ZERO,
                )
            },
        )
    }
}