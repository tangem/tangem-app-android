package com.tangem.data.visa.utils

import com.tangem.datasource.api.pay.models.response.VisaTxHistoryResponse
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.utils.converter.Converter

internal object VisaTxHistoryItemConverter : Converter<VisaTxHistoryResponse.Transaction, VisaTxHistoryItem> {

    override fun convert(value: VisaTxHistoryResponse.Transaction): VisaTxHistoryItem {
        return VisaTxHistoryItem(
            id = value.transactionId.toString(),
            date = value.transactionDt,
            amount = value.blockchainAmount,
            fiatAmount = value.transactionAmount,
            merchantName = value.merchantName,
            status = value.transactionStatus,
            fiatCurrency = findCurrencyByNumericCode(value.transactionCurrencyCode),
        )
    }
}