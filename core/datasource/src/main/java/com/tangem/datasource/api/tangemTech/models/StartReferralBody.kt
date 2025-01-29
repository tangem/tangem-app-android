package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Body for make user referral */
@JsonClass(generateAdapter = true)
data class StartReferralBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "networkId") val networkId: String,
    @Json(name = "tokenId") val tokenId: String,
    @Json(name = "address") val address: String,
)