package com.tangem.data.visa.utils

import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.utils.converter.Converter

internal object TangemPayTxHistoryItemStatusConverter : Converter<String, TangemPayTxHistoryItem.Status> {
    override fun convert(value: String): TangemPayTxHistoryItem.Status {
        return when (value.uppercase()) {
            "PENDING" -> TangemPayTxHistoryItem.Status.PENDING
            "RESERVED" -> TangemPayTxHistoryItem.Status.RESERVED
            "COMPLETED" -> TangemPayTxHistoryItem.Status.COMPLETED
            "DECLINED" -> TangemPayTxHistoryItem.Status.DECLINED
            else -> TangemPayTxHistoryItem.Status.UNKNOWN
        }
    }
}