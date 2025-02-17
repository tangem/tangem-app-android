package com.tangem.data.visa.utils

import com.tangem.datasource.api.visa.models.response.VisaTxHistoryResponse
import com.tangem.domain.visa.model.VisaTxHistoryItem

internal class VisaTxHistoryItemFactory {

    fun create(transaction: VisaTxHistoryResponse.Transaction): VisaTxHistoryItem {
        return VisaTxHistoryItem(
            id = transaction.transactionId.toString(),
            date = transaction.transactionDt,
            amount = transaction.blockchainAmount,
            fiatAmount = transaction.transactionAmount,
            merchantName = transaction.merchantName,
            status = transaction.transactionStatus,
            fiatCurrency = findCurrencyByNumericCode(transaction.transactionCurrencyCode),
        )
    }
}