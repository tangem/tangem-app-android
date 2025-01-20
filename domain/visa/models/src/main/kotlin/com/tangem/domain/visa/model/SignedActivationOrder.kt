package com.tangem.domain.visa.model

data class SignedActivationOrder(
    val activationOrder: ActivationOrder,
    val signature: String,
)