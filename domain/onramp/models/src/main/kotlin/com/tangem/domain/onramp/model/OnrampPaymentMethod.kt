package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampPaymentMethod(
    val id: String,
    val name: String,
    val imageUrl: String,
)