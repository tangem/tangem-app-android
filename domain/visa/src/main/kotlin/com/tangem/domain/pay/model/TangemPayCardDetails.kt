package com.tangem.domain.pay.model

/**
 * Returned dynamically to show on the UI. Never saved anywhere
 */
data class TangemPayCardDetails(
    val pan: String,
    val cvv: String,
    val expirationYear: String,
    val expirationMonth: String,
)