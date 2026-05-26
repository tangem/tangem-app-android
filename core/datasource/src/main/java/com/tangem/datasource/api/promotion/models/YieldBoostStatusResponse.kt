package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldBoostStatusResponse(
    @Json(name = "tokenName") val tokenName: String?,
    @Json(name = "networkId") val networkId: String?,
    @Json(name = "moduleAddress") val moduleAddress: String?,
    @Json(name = "userAddress") val userAddress: String?,
    @Json(name = "contractAddress") val contractAddress: String?,
    @Json(name = "promoEnrollmentStatus") val promoEnrollmentStatus: String,
    @Json(name = "activationDate") val activationDate: String?,
    @Json(name = "qualificationEndDate") val qualificationEndDate: String?,
    @Json(name = "disqualificationReason") val disqualificationReason: String?,
)