package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaActivationInput(
    @Json(name = "cardId") val cardId: String,
    @Json(name = "cardPublicKey") val cardPublicKey: String,
    @Json(name = "isAccessCodeSet") val isAccessCodeSet: Boolean,
)