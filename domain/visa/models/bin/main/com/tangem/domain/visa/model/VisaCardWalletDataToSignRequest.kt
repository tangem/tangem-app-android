package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaCardWalletDataToSignRequest(
    @Json(name = "activationOrderInfo") val activationOrderInfo: VisaActivationOrderInfo,
    @Json(name = "cardWalletAddress") val cardWalletAddress: String,
)