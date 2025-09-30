package com.tangem.domain.visa.model

import org.joda.time.DateTime
import java.math.BigDecimal

data class TangemPayTxHistoryItem(
    val id: String,
    val date: DateTime?,
    val amount: BigDecimal?,
    val merchantName: String?,
    val status: String?,
    val currency: String?,
) {
    val timeStampInMillis: Long = date?.millis ?: 0
}