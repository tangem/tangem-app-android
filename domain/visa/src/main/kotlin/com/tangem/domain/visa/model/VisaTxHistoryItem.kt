package com.tangem.domain.visa.model

import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.Currency

data class VisaTxHistoryItem(
    val id: String,
    val date: DateTime,
    val amount: BigDecimal,
    val fiatAmount: BigDecimal,
    val merchantName: String?,
    val status: String,
    val fiatCurrency: Currency,
)