package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CashbackHistoryResponse
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object CashbackHistoryConverter : Converter<CashbackHistoryResponse, CashbackHistory> {

    override fun convert(value: CashbackHistoryResponse): CashbackHistory {
        return CashbackHistory(
            currency = value.result?.currency.orEmpty(),
            months = value.result?.items.orEmpty().map { item ->
                CashbackHistory.MonthlyCashback(
                    year = item.year,
                    month = item.month,
                    confirmedAmount = item.confirmedAmount ?: BigDecimal.ZERO,
                )
            },
        )
    }
}