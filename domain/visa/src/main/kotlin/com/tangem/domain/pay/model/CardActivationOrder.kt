package com.tangem.domain.pay.model

data class CardActivationOrder(
    val productInstanceId: String,
    val lastFourDigits: String,
)