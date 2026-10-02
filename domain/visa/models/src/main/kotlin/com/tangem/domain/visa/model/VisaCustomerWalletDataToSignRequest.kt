package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaCustomerWalletDataToSignRequest(
    val orderId: String,
    val cardWalletAddress: String,
    val customerWalletAddress: String,
)