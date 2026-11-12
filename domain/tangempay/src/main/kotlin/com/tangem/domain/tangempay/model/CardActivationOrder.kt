package com.tangem.domain.tangempay.model

data class CardActivationOrder(
    val productInstanceId: String,
    val lastFourDigits: String,
)