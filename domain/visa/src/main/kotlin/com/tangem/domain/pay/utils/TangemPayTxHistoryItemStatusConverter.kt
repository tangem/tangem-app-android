package com.tangem.domain.pay.utils

import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter

object TangemPayTxHistoryItemStatusConverter : Converter<String, TangemPayTxHistoryItem.Status> {
    override fun convert(value: String): TangemPayTxHistoryItem.Status {
        return when (value.uppercase()) {
            "PENDING" -> TangemPayTxHistoryItem.Status.PENDING
            "RESERVED" -> TangemPayTxHistoryItem.Status.RESERVED
            "COMPLETED" -> TangemPayTxHistoryItem.Status.COMPLETED
            "DECLINED" -> TangemPayTxHistoryItem.Status.DECLINED
            "REVERSED" -> TangemPayTxHistoryItem.Status.REVERSED
            else -> TangemPayTxHistoryItem.Status.UNKNOWN
        }
    }
}