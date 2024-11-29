package com.tangem.features.onramp.paymentmethod.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class PaymentMethodUM(
    val id: String,
    val imageUrl: String,
    val name: String,
    val onSelect: () -> Unit,
)