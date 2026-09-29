package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WaitlistRequest(
    @SerialName("email") val email: String,
    @SerialName("product") val product: String,
    @SerialName("country") val country: String,
    @SerialName("region") val region: String?,
    @SerialName("marketing_consent") val hasMarketingConsent: Boolean,
)