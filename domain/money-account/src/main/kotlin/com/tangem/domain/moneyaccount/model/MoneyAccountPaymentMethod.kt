package com.tangem.domain.moneyaccount.model

data class MoneyAccountPaymentMethod(
    val id: String,
    val name: String,
    val countryCode: String,
    val iconUrl: String?,
)