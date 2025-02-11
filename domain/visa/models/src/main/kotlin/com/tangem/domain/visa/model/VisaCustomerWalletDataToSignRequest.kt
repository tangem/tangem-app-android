package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaCustomerWalletDataToSignRequest(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "card_wallet_address") val cardWalletAddress: String,
)