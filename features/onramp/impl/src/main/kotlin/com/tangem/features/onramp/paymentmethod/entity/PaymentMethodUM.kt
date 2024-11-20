package com.tangem.features.onramp.paymentmethod.entity

data class PaymentMethodUM(
    val id: String,
    val image: String,
    val name: String,
    val onSelect: () -> Unit,
)
