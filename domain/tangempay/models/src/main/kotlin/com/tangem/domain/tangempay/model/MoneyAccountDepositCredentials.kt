package com.tangem.domain.tangempay.model

data class MoneyAccountDepositCredentials(
    val address: String,
    val signature: String,
)