package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.CashbackHistory
import com.tangem.spend.datasource.tangempay.models.response.CashbackHistoryResponse
import com.tangem.utils.converter.Converter

internal object CashbackHistoryConverter : Converter<CashbackHistoryResponse, CashbackHistory> {

    override fun convert(value: CashbackHistoryResponse): CashbackHistory {
        return CashbackHistory(
            months = value.result?.items.orEmpty().map { item ->
                CashbackHistory.MonthlyCashback(
                    year = item.year,
                    month = item.month,
                    confirmedAmount = item.confirmedAmount,
                    currency = item.currency.takeIf(String::isNotBlank) ?: "USD",
                )
            },
        )
    }
}