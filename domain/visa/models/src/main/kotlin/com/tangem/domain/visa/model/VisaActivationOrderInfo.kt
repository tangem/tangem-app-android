package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaActivationOrderInfo(
    val orderId: String,
    val customerId: String,
    val customerWalletAddress: String,
    val cardWalletAddress: String?,
)