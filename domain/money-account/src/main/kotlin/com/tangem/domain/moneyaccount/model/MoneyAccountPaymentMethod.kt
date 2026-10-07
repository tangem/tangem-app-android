package com.tangem.domain.moneyaccount.model

import java.math.BigDecimal

data class MoneyAccountPaymentMethod(
    val id: String,
    val name: String,
    val countryCode: String,
    val iconUrl: String?,
    val receiveFee: Fee?,
    val sendFee: Fee?,
) {

    data class Fee(
        val type: String,
        val amount: BigDecimal,
        val currency: String,
    )
}