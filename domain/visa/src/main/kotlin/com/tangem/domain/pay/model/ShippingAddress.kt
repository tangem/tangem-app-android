package com.tangem.domain.pay.model

data class ShippingAddress(
    val firstName: String,
    val lastName: String,
    val email: String,
    val region: String,
    val city: String,
    val line1: String,
    val line2: String?,
    val postalCode: String,
    val phone: String,
)